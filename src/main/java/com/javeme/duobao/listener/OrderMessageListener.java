    package com.javeme.duobao.listener;

    import com.javeme.duobao.entity.*;
    import com.javeme.duobao.repository.*;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.amqp.rabbit.annotation.RabbitListener;
    import org.springframework.amqp.rabbit.core.RabbitTemplate;
    import org.springframework.data.redis.core.StringRedisTemplate;
    import org.springframework.stereotype.Component;
    import org.springframework.transaction.annotation.Transactional;

    import java.math.BigDecimal;
    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Optional;

    @Component
    @RequiredArgsConstructor
    @Slf4j
    public class OrderMessageListener {

        private final OrderRepository orderRepository;
        private final OrderItemRepository orderItemRepository;
        private final ProductRepository productRepository;
        private final AddressBookRepository addressBookRepository;
        private final UserRepository userRepository;
        private final StringRedisTemplate stringRedisTemplate;
        private final RabbitTemplate rabbitTemplate;

        @RabbitListener(queues = "flash.sale.order.queue")
        @Transactional
        public void processOrder(OrderMessage orderMessage) {

            Optional<Order> existingOrder = orderRepository.findByOrderNumber(orderMessage.getOrderNumber());

            if (existingOrder.isPresent()) {
                log.warn("Duplicate message detected: {}. Skipping gracefully.", orderMessage.getOrderNumber());
                return;
            }
            try {
                log.info("Started processing new order: {}", orderMessage.getOrderNumber());


                Product product = productRepository.findById(orderMessage.getProductId()).orElseThrow(()
                        -> new RuntimeException("Product not found"));

                AddressBook address = addressBookRepository.findById(orderMessage.getAddressBookId()).orElseThrow(()
                        -> new RuntimeException("Address not found"));

                User user = userRepository.findById(orderMessage.getUserId()).orElseThrow(()
                        -> new RuntimeException("User not found"));

                // 2. Calculate totalAmount.
                BigDecimal totalAmount = product.getPrice().multiply(new BigDecimal(orderMessage.getQuantity()));
                // 3. Build and save the Order entity.
                Order order = new Order();
                order.setStatus(Order.TO_BE_CONFIRMED);
                order.setOrderNumber(orderMessage.getOrderNumber());
                order.setAddressBookId(orderMessage.getAddressBookId());
                order.setOrderTime(LocalDateTime.now());
                order.setPayStatus(Order.UN_PAID);
                order.setAmount(totalAmount);
                order.setRemark(orderMessage.getRemark());
                order.setUserId(orderMessage.getUserId());
                order.setUsername(user.getUsername());
                order.setPhone(user.getPhone());
                order.setAddress(address.getAddress());
                order.setPostcode(address.getPostcode());
                order.setConsignee(address.getConsignee());
                Order saveOrder = orderRepository.save(order);


                // 4. Build and save the OrderItem entity.
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(saveOrder.getId());
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getProductName());
                orderItem.setProductImage(product.getImage());
                orderItem.setQuantity(orderMessage.getQuantity());
                orderItem.setAmount(product.getPrice());

                orderItemRepository.save(orderItem);
                log.info("Order successfully finalized: {}", orderMessage.getOrderNumber());
                rabbitTemplate.convertAndSend("order.delay.queue", saveOrder.getId());
                log.info("Order {} sent to delay queue. 60-second countdown started.", orderMessage.getOrderNumber());
            } catch (Exception e) {
                log.error("Error processing order: {}", orderMessage.getOrderNumber());
                String redisKey = "stock:product:" + orderMessage.getProductId();
                stringRedisTemplate.opsForValue().increment(redisKey, orderMessage.getQuantity());

                log.info("Redis stock restored for key: {}", redisKey);
                throw e;
            }
        }

        @RabbitListener(queues = "order.release.queue")
        @Transactional
        public void releaseOrder(Long orderId) {

            log.info("Checking if order {} is paid...", orderId);

            Optional<Order> order = orderRepository.findById(orderId);

            if (order.isEmpty()) {
                return;
            }

            Order existingOrder = order.get();

            Integer currentPayStatus = existingOrder.getPayStatus();
            if (currentPayStatus == null) {
                currentPayStatus = Order.UN_PAID;
            }

            if (Order.PAID.equals(currentPayStatus)) {

                log.info("Order {} is already PAID. No action needed.", existingOrder.getOrderNumber());
                return;
            }

            if (Order.UN_PAID.equals(currentPayStatus) && !Order.CANCELLED.equals(existingOrder.getStatus())) {
                existingOrder.setStatus(Order.CANCELLED);
                existingOrder.setCancelTime(LocalDateTime.now());
                orderRepository.save(existingOrder);
                log.info("Order {} CANCELLED due to timeout.", existingOrder.getOrderNumber());

                List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

                for (OrderItem item : items) {
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    if (product == null) continue;

                    if (Boolean.TRUE.equals(product.getIsFlashSale())) {
                        String key = "stock:product:" + product.getId();
                        stringRedisTemplate.opsForValue().increment(key, item.getQuantity());
                        log.info("Flash Sale detected. Restored {} of product {} to Redis.", item.getQuantity(), product.getProductName());
                    } else {
                        product.setStock(product.getStock() + item.getQuantity());
                        productRepository.save(product);
                        log.info("Standard Order detected. Restored {} of product {} to MySQL.", item.getQuantity(), product.getProductName());
                    }
                  }
                } else {
                    log.info("Order {} is already PAID or CANCELLED. Skipping rollback.", existingOrder.getOrderNumber());
                }
            }
        }

