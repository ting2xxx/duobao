package com.javeme.duobao.service;

import com.javeme.duobao.common.BaseContext;
import com.javeme.duobao.dto.ProductDTO;
import com.javeme.duobao.entity.Product;
import com.javeme.duobao.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminProductService {

    private final ProductRepository productRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    public void publishFlashSales(Long productId) {
        //Find the flash sale product by productId
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new RuntimeException("Product not found"));

        //if the product is already on flash sale, throw exception
        if (Boolean.TRUE.equals(product.getIsFlashSale())) {
            throw new RuntimeException("This product is already on flash sale");
        }

        //set product to flash sale
        product.setIsFlashSale(true);
        productRepository.save(product);

        //generate a redis key for the product
        String key = "stock:product:" + productId;

        //save the stock of the product to redis
        stringRedisTemplate.opsForValue().set(key, String.valueOf(product.getStock()));

        log.info("Flash sale published for product: {}, Initial Stock loaded to Redis: {}", productId,
                product.getStock());
    }

    @Transactional
    public void endFlashSales(Long productId) {

        //find product with productId
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new RuntimeException("Product not found:"));

        //check whether product is on flash sale, if not throw exception
        if (!Boolean.TRUE.equals(product.getIsFlashSale())) {
            throw new RuntimeException("This product is not on flash sale");
        }

        //set product to not on flash sale
        product.setIsFlashSale(false);
        productRepository.save(product);

        //generate a key for the product
        String key = "stock:product:" + productId;
        //delete the existing key in redis
        stringRedisTemplate.delete(key);

        log.info("Flash sale ended for product: {}", productId);
    }

    @Transactional
    public void updateStock(Long productId, Integer quantity) {

        String key = "stock:product:" + productId;
        //find product with product id
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new RuntimeException("Product not found"));

        //set stock for the product
        product.setStock(quantity);
        Product savedProduct = productRepository.save(product);

        rabbitTemplate.convertAndSend("product.cache.eviction.queue", product.getCategoryId());
        //if the product is on flashSale
        Boolean isFlashSale = savedProduct.getIsFlashSale();
        if (Boolean.TRUE.equals(isFlashSale)) {

            //add the product to flash sale, so other can still buy the product with flashSale item
            stringRedisTemplate.opsForValue().set(key, quantity.toString());
        }
    }

    public void addProduct(ProductDTO productDTO) {

        //create a product object
        Product product = new Product();

        //copy everything from productDTO to product
        BeanUtils.copyProperties(productDTO, product);
        product.setCreateUser(BaseContext.getCurrentID());
        product.setCreateTime(LocalDateTime.now());
        product.setStatus(1);
        productRepository.save(product);
    }
}
