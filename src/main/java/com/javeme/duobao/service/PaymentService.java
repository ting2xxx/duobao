package com.javeme.duobao.service;

import com.javeme.duobao.entity.Order;
import com.javeme.duobao.entity.OrderItem;
import com.javeme.duobao.entity.Product;
import com.javeme.duobao.entity.User;
import com.javeme.duobao.repository.OrderItemRepository;
import com.javeme.duobao.repository.OrderRepository;
import com.javeme.duobao.repository.ProductRepository;
import com.javeme.duobao.repository.UserRepository;
import com.javeme.duobao.vo.OrderVO;
import com.javeme.duobao.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

//    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public UserVO topup(Long userId, BigDecimal amount) {
        User currentUser = userRepository.findById(userId).orElseThrow(() ->
                new RuntimeException("User not found"));
        if (currentUser.getBalance() == null) {
            currentUser.setBalance(BigDecimal.ZERO);
        }
        currentUser.setBalance(currentUser.getBalance().add(amount));
        userRepository.save(currentUser);
        return UserVO.builder()
                .id(currentUser.getId())
                .balance(currentUser.getBalance())
                .build();
    }

    @Transactional
    public OrderVO pay(String orderNumber) {

        String key = "lock:pay:order:" + orderNumber;
        Boolean acquiredLock = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquiredLock)) {
            throw new RuntimeException("Payment is currently processing. Please do not double-click.");
        }
        try {
            Order currentOrder = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() ->
                    new RuntimeException("Order not found"));
            if (currentOrder.getStatus() == Order.CANCELLED) {
                throw new RuntimeException("Order has been expired");
            }

            if (currentOrder.getPayStatus() == Order.PAID) {
                throw new RuntimeException("Order has been paid");
            }
            Long userId = currentOrder.getUserId();
            User currentUser = userRepository.findById(userId).orElseThrow(() ->
                    new RuntimeException("User not found"));

            if (currentUser.getBalance().compareTo(currentOrder.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            currentUser.setBalance(currentUser.getBalance().subtract(currentOrder.getAmount()));

            List<OrderItem> orderItems = orderItemRepository.findByOrderId(currentOrder.getId());
            for (OrderItem orderItem : orderItems) {

                Product product = productRepository.findById(orderItem.getProductId()).orElseThrow(() ->
                        new RuntimeException("Product not found"));

                if (Boolean.TRUE.equals(product.getIsFlashSale())) {

                    int updatedRows = productRepository.deductStock(orderItem.getProductId(), orderItem.getQuantity());

                    if (updatedRows == 0) {
                        throw new RuntimeException("Payment failed: Out of stock");
                    }
                }
            }
            userRepository.save(currentUser);
            currentOrder.setPayStatus(Order.PAID);
            currentOrder.setStatus(Order.CONFIRMED);
            currentOrder.setPayTime(LocalDateTime.now());
            orderRepository.save(currentOrder);
            return OrderVO.builder()
                    .orderNumber(currentOrder.getOrderNumber())
                    .status(currentOrder.getStatus())
                    .build();

        } finally {
            //unlock
            stringRedisTemplate.delete(key);
        }
    }

    @Transactional
    public void paySuccess(String orderNumber) {

        //Find with orderNumber
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() ->
                new RuntimeException("Order not found"));

        //Check whether order status is canceled
        if (order.getStatus().equals(Order.CANCELLED)) {
            throw new RuntimeException("Order was already canceled due to timeout");
        }

        //set pay status to paid, status to confirmed
        order.setPayStatus(Order.PAID);
        order.setStatus(Order.CONFIRMED);
        order.setPayTime(LocalDateTime.now());

        orderRepository.save(order);

        log.info("Order {} payment confirmed. Cancellation avoided.", orderNumber);
    }

    public String initiatePayment(Long userId, String orderNumber) {

        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId).orElseThrow(() ->
                new RuntimeException("Order not found"));

        if (!order.getStatus().equals(Order.TO_BE_CONFIRMED)) {
            throw new RuntimeException("This order cannot be paid (Status: " + order.getStatus() + ")");
        }

        String mockPaymentUrl = "http://localhost:3000/pay?order=" +orderNumber + "&amount=" + order.getAmount();

        log.info("User {} initiated payment for order {}. Amount: {}", userId, orderNumber, order.getAmount());

        return mockPaymentUrl;
    }
}
