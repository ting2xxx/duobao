package com.javeme.duobao.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javeme.duobao.entity.Product;
import com.javeme.duobao.repository.ProductRepository;
import com.javeme.duobao.vo.ProductVO;
import com.rabbitmq.tools.json.JSONUtil;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    public List<ProductVO> getActiveProductsByCategory(Long categoryId){

        String key = "cache:products:category:" + categoryId;
        //get the key from redis
        String cachedJson = stringRedisTemplate.opsForValue().get(key);
        if (cachedJson != null) {
            // ObjectMapper turns the JSON String back into a List<ProductVO>
            try {
                return objectMapper.readValue(cachedJson, new TypeReference<List<ProductVO>>() {
                });
            } catch (Exception e) {
                throw new RuntimeException("Error reading cache data for category: " + categoryId);
            }
        }

        // 1. Get the raw entities from the database
        List<Product> products = productRepository.findByCategoryIdAndStatus(categoryId, 1);

        //if product list is null or empty, return an empty list
        if (products == null || products.isEmpty()) {
            stringRedisTemplate.opsForValue().set(key, "[]", 2, TimeUnit.MINUTES);
            return new ArrayList<>();
        }
        // 2. Use Stream and map() to convert them cleanly!
        List<ProductVO> voList =  products.stream()
                .map(product -> {
                    ProductVO productVO = new ProductVO();
                    // BeanUtils copies all matching fields (name, price, etc.) instantly!
                    BeanUtils.copyProperties(product, productVO);
                    return productVO;
                })
                        .collect(Collectors.toList());

        try {
            //Convert volist to string
            String jsonToCache = objectMapper.writeValueAsString(voList);
            //save the products string into redis
            stringRedisTemplate.opsForValue().set(key, jsonToCache, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("Error saving cache data for category: " + categoryId);
        }
        return voList;
    }

    public List<ProductVO> getTopSellingProducts(List<Long> productIds) {

        // 1. Hit the database ONCE to get all products
        List<Product> products = productRepository.findAllById(productIds);

        // 2. Create a dictionary mapping the Product ID to the Product object
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        List<ProductVO> resultList = new ArrayList<>();

        for (Long productId : productIds) {
            Product product = productMap.get(productId); // Instantly find the product
            if (product != null && Boolean.FALSE.equals(product.getIsDeleted())) { // Make sure it's not soft-deleted!
                ProductVO productVO = new ProductVO();
                BeanUtils.copyProperties(product, productVO);
                resultList.add(productVO);
            }
        }

        return resultList;
    }
}
