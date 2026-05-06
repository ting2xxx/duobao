package com.javeme.duobao.service;

import com.javeme.duobao.entity.Product;
import com.javeme.duobao.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminProductService {

    private final ProductRepository productRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public void publishFlashSales(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new RuntimeException("Product not found"));

        if (Boolean.TRUE.equals(product.getIsFlashSale())) {
            throw new RuntimeException("This product is already on flash sale");
        }

        product.setIsFlashSale(true);
        productRepository.save(product);

        String key = "stock:product:" + productId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(product.getStock()));

        log.info("Flash sale published for product: {}, Initial Stock loaded to Redis: {}", productId,
                product.getStock());
    }

    @Transactional
    public void endFlashSales(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new RuntimeException("Product not found:"));

        if (!Boolean.TRUE.equals(product.getIsFlashSale())) {
            throw new RuntimeException("This product is not on flash sale");
        }

        product.setIsFlashSale(false);
        productRepository.save(product);

        String key = "stock:product:" + productId;
        stringRedisTemplate.delete(key);

        log.info("Flash sale ended for product: {}", productId);
    }

    @Transactional
    public void updateStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new RuntimeException("Product not found"));

        product.setStock(quantity);
        Product savedProduct = productRepository.save(product);

        Boolean isFlashSale = savedProduct.getIsFlashSale();
        if (Boolean.TRUE.equals(isFlashSale)) {
            String key = "stock:product:" + productId;
            stringRedisTemplate.opsForValue().set(key, quantity.toString());
        }

    }
}
