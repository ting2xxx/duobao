package com.javeme.duobao.service;

import com.javeme.duobao.entity.Order;
import com.javeme.duobao.entity.OrderItem;
import com.javeme.duobao.entity.Product;
import com.javeme.duobao.repository.OrderItemRepository;
import com.javeme.duobao.repository.OrderRepository;
import com.javeme.duobao.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public void refund(String orderNumber) {

        //get order with orderNumber
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() ->
                new RuntimeException("Order not found"));

        //if order status = unpaid, throw exception
        if (order.getStatus().equals(Order.UN_PAID)) {

            throw new RuntimeException("Unpaid orders should be canceled, not refund");
        }

        //set payStatus to refund, set status to cancelled, set cancel time, save
        order.setPayStatus(Order.REFUND);
        order.setStatus(Order.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        orderRepository.save(order);

        //restock item, find order item with order id
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        //loop the order items
        for (OrderItem item : items) {

            //find product with product id
            Product product = productRepository.findById(item.getProductId()).orElseThrow(() ->
                    new RuntimeException("Product not found"));

            //if product status is flashSale, increase redis key so other can buy immediately.
            if (Boolean.TRUE.equals(product.getIsFlashSale())) {

                String key = "stock:product:" + product.getId();
                stringRedisTemplate.opsForValue().increment(key, item.getQuantity());
                log.info("Refund: Restored {} to Redis for Flash Sale product {}", item.getQuantity(), product.getId());
            } else {
                //if it is not, increate stock in database
                productRepository.increaseStock(product.getId(), item.getQuantity());
                log.info("Refund: Restored {} to MySQL for standard product {}", item.getQuantity(), product.getId());
            }
        }
    }
}
