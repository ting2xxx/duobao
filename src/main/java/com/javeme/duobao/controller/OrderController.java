package com.javeme.duobao.controller;

import com.javeme.duobao.dto.OrderDTO;
import com.javeme.duobao.entity.OrderMessage;
import com.javeme.duobao.entity.Product;
import com.javeme.duobao.repository.ProductRepository;
import com.javeme.duobao.service.FlashSalesOrderService;
import com.javeme.duobao.service.OrderService;
import com.javeme.duobao.service.StandardOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Queue;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final FlashSalesOrderService flashSalesOrderService;
    private final ProductRepository productRepository;
    private final StandardOrderService standardOrderService;
    private final Queue flashSaleOrderQueue;
    private final OrderService orderService;

    @PostMapping("/submit")
    public ResponseEntity<String> submit(@RequestBody OrderDTO orderDTO) {
        Product product = productRepository.findById(orderDTO.getProductId()).orElseThrow(() ->
                new RuntimeException("Product not found"));

        if (Boolean.TRUE.equals(product.getIsFlashSale())) {
            flashSalesOrderService.submitAsync(orderDTO);
            return ResponseEntity.ok("Flash Sale order is queuing. Please wait.");

        } else {
            standardOrderService.submitSync(orderDTO);
            return ResponseEntity.ok("Order placed successfully!");
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<String> checkout(@RequestHeader("userId") Long userId) {

        orderService.checkout(userId);
        return ResponseEntity.ok("Order placed successfully!");
    }
}
