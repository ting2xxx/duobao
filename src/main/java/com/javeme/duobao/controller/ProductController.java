package com.javeme.duobao.controller;

import com.javeme.duobao.service.ProductService;
import com.javeme.duobao.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductVO>> getActiveProductsByCategory (@PathVariable Long categoryId) {
        List<ProductVO> products = productService.getActiveProductsByCategory(categoryId);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/top-selling")
    public ResponseEntity<List<ProductVO>> getTopSellingProducts() {

        String key = "product:top_selling";

        // 1. Get the Top 10 IDs from Redis (returns a Set of Strings)
        Set<String> productSet = stringRedisTemplate.opsForZSet().reverseRange(key, 0, 9);
        //convert set to list
        List<Long> productIds = productSet.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // 3. Pass the clean List to the Service
        List<ProductVO> products = productService.getTopSellingProducts(productIds);
        return ResponseEntity.ok(products);
    }
}
