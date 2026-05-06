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
            rabbitTemplate.convertAndSend("order.delay.queue", orderMessage);
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
    public void releaseOrder(OrderMessage orderMessage) {

        log.info("Checking if order {} is paid...", orderMessage.getOrderNumber());

        Optional<Order> order = orderRepository.findByOrderNumber(orderMessage.getOrderNumber());

        if (order.isEmpty()) {
            return;
        }

        Order existingOrder = order.get();

        if (existingOrder.getPayStatus() == Order.PAID) {

            log.info("PAID");

        } else if (existingOrder.getPayStatus() == Order.UN_PAID
                && existingOrder.getStatus() != Order.CANCELLED) {

            existingOrder.setStatus(Order.CANCELLED);
            existingOrder.setCancelTime(LocalDateTime.now());
            orderRepository.save(existingOrder);
            log.info("Order {} CANCELLED due to timeout.", existingOrder.getOrderNumber());

            Product product = productRepository.findById(orderMessage.getProductId()).orElseThrow(() ->
                    new RuntimeException("Product not found"));

            if (product == null) {
                return;
            }

            if (Boolean.TRUE.equals(product.getIsFlashSale())) {

                String key = "stock:product:" + product.getId();
                stringRedisTemplate.opsForValue().increment(key, orderMessage.getQuantity());
                log.info("Flash Sale detected. Restored {} to Redis.", orderMessage.getQuantity());

            } else {

                productRepository.increaseStock(product.getId(), orderMessage.getQuantity());
                log.info("Standard Order detected. Restored {} to MySQL.", orderMessage.getQuantity());
            }
        } else {

            log.info("Order {} is already PAID or CANCELLED. Skipping rollback.", existingOrder.getOrderNumber());
        }
    }
}
