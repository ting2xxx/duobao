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

        //find user byUserId
        User currentUser = userRepository.findById(userId).orElseThrow(() ->
                new RuntimeException("User not found"));

        //if user balance is null, set the balance to 0
        if (currentUser.getBalance() == null) {
            currentUser.setBalance(BigDecimal.ZERO);
        }

        //else set balance to current balance + amount
        currentUser.setBalance(currentUser.getBalance().add(amount));

        //save user
        userRepository.save(currentUser);

        //return VO
        return UserVO.builder()
                .id(currentUser.getId())
                .balance(currentUser.getBalance())
                .build();
    }

    @Transactional
    public OrderVO pay(String orderNumber) {

        //generate lock key
        String key = "lock:pay:order:" + orderNumber;

        //set a 10 seconds key (set if absent)
        Boolean acquiredLock = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);

        //if lock == false, throw exception
        if (Boolean.FALSE.equals(acquiredLock)) {
            throw new RuntimeException("Payment is currently processing. Please do not double-click.");
        }

        try {
            //find order with orderNumber
            Order currentOrder = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() ->
                    new RuntimeException("Order not found"));
            //order status is canceled, throw exception
            if (currentOrder.getStatus() == Order.CANCELLED) {
                throw new RuntimeException("Order has been expired");
            }
            //if order pay status is paid, throw exception
            if (currentOrder.getPayStatus() == Order.PAID) {
                throw new RuntimeException("Order has been paid");
            }
            //find user by userId
            Long userId = currentOrder.getUserId();
            User currentUser = userRepository.findById(userId).orElseThrow(() ->
                    new RuntimeException("User not found"));

            //if user balance is less than order amount, throw exception
            if (currentUser.getBalance().compareTo(currentOrder.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            //else set balance to current balance - order amount
            currentUser.setBalance(currentUser.getBalance().subtract(currentOrder.getAmount()));

            //find orderItem with orderId
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(currentOrder.getId());
            for (OrderItem orderItem : orderItems) {

                //find product with productId
                Product product = productRepository.findById(orderItem.getProductId()).orElseThrow(() ->
                        new RuntimeException("Product not found"));

                //if product is flash sale item
                if (Boolean.TRUE.equals(product.getIsFlashSale())) {

                    //deduct stock
                    int updatedRows = productRepository.deductStock(orderItem.getProductId(), orderItem.getQuantity());

                    //if updatedRows == 0 throws exception
                    if (updatedRows == 0) {
                        throw new RuntimeException("Payment failed: Out of stock");
                    }
                }
            }

            //save user
            userRepository.save(currentUser);
            //update order status
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
        //check pay status is paid
        if (order.getPayStatus().equals(Order.PAID)) {
            return;
        }

        //set pay status to paid, status to confirmed
        order.setPayStatus(Order.PAID);
        order.setStatus(Order.CONFIRMED);
        order.setPayTime(LocalDateTime.now());

        orderRepository.save(order);

        log.info("Order {} payment confirmed. Cancellation avoided.", orderNumber);
    }

    public String getPaymentLink(Long userId, String orderNumber) {

        //find order by orderNumber and userId
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId).orElseThrow(() ->
                new RuntimeException("Order not found"));
        //if status is not to be confirmed, throw exception
        if (!order.getStatus().equals(Order.TO_BE_CONFIRMED)) {
            throw new RuntimeException("This order cannot be paid (Status: " + order.getStatus() + ")");
        }
        //generate a mockPaymentUrl
        String mockPaymentUrl = "http://localhost:3000/pay?order=" +orderNumber + "&amount=" + order.getAmount();


        return mockPaymentUrl;
    }
}
