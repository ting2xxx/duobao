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

        /**
         * Process Order
         *
         * @param orderMessage
         */
        @RabbitListener(queues = "flash.sale.order.queue")
        @Transactional
        public void processOrder(OrderMessage orderMessage) {

            // Check if order already exists to prevent duplicate processing
            orderRepository.findByOrderNumber(orderMessage.getOrderNumber())
                    .ifPresent(existingOrder -> {
                        throw new RuntimeException("Order already exists");
                    });

            try {
                log.info("Started processing new order: {}", orderMessage.getOrderNumber());

                //check product, address and user exist
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

                //once Order and Order item saved, send a message to rabbitMQ to wait for 15 minutes
                rabbitTemplate.convertAndSend("order.delay.queue", saveOrder.getId());

                log.info("Order {} sent to delay queue. 15 minutes countdown started.", orderMessage.getOrderNumber());

            } catch (Exception e) {

                log.error("Error processing order: {}", orderMessage.getOrderNumber());
                //if order failed, add back the stock back to the redis key
                String redisKey = "stock:product:" + orderMessage.getProductId();
                stringRedisTemplate.opsForValue().increment(redisKey, orderMessage.getQuantity());

                log.info("Redis stock restored for key: {}", redisKey);
                throw e;
            }
        }

        /**
         * Release Order
         *
         * @param orderId
         */
        @RabbitListener(queues = "order.release.queue")
        @Transactional
        public void releaseOrder(Long orderId) {

            log.info("Checking if order {} is paid...", orderId);
            //find order by id
            Order existingOrder = orderRepository.findById(orderId).orElseThrow(() ->
                    new RuntimeException("Order not found"));

            //if order pay status is null, order is unpaid
            Integer currentPayStatus = existingOrder.getPayStatus();
            if (currentPayStatus == null) {
                currentPayStatus = Order.UN_PAID;
            }
            //if the pay status is paid, return
            if (Order.PAID.equals(currentPayStatus)) {

                log.info("Order {} is already PAID. No action needed.", existingOrder.getOrderNumber());
                return;
            }

            //if order is unpaid and status is not canceled, cancel it
            if (Order.UN_PAID.equals(currentPayStatus) && !Order.CANCELLED.equals(existingOrder.getStatus())) {
                existingOrder.setStatus(Order.CANCELLED);
                existingOrder.setCancelTime(LocalDateTime.now());
                orderRepository.save(existingOrder);
                log.info("Order {} CANCELLED due to timeout.", existingOrder.getOrderNumber());

                //find order items by orderId
                List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

                for (OrderItem item : items) {
                    //find product by productId
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    //if product is null, continue
                    if (product == null) continue;

                    //if product is flashSale item, increase the stock based on order item
                    if (Boolean.TRUE.equals(product.getIsFlashSale())) {
                        String key = "stock:product:" + product.getId();
                        stringRedisTemplate.opsForValue().increment(key, item.getQuantity());
                        log.info("Flash Sale detected. Restored {} of product {} to Redis.", item.getQuantity(), product.getProductName());

                        //if product is not flashSale, update the stock based on orderItem
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

        @RabbitListener(queues = "product.cache.eviction.queue")
        public void evictProductCache(Long categoryId) {
            stringRedisTemplate.delete("cache:products:category:" + categoryId);
        }
    }

