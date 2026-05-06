package com.javeme.duobao.service;

import com.javeme.duobao.dto.CartItemDTO;
import com.javeme.duobao.entity.Product;
import com.javeme.duobao.repository.ProductRepository;
import com.javeme.duobao.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ProductRepository productRepository;

    public void addCart(Long userId, CartItemDTO cartItemDTO) {
        String key = "cart:user" + userId;
        String productId = cartItemDTO.getProductId().toString();

        Product product = productRepository.findById(cartItemDTO.getProductId()).orElseThrow(() ->
                new RuntimeException("Product not found"));

        Object currentObj = stringRedisTemplate.opsForHash().get(key, productId);
        int currentInCart = (currentObj == null) ? 0 : Integer.parseInt(currentObj.toString());

        int total = currentInCart + cartItemDTO.getQuantity();

        if (total > product.getStock()) {

            throw new RuntimeException("Limit reached! You already have " + currentInCart + " items in your cart");
        }

        if (total <= 0) {
            stringRedisTemplate.opsForHash().delete(key, productId);

        } else {

            stringRedisTemplate.opsForHash().put(key, productId, String.valueOf(total));
        }
    }

    public List<CartVO> getCart(Long userId) {
        String key = "cart:user" + userId;

        // 1. Get everything from the Redis Hash (Map<ProductId, Quantity>)
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Extract IDs and fetch all products from DB in ONE query
        List<Long> productIds = entries.keySet().stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());

        List<Product> products = productRepository.findAllById(productIds);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 3. Assemble the VO list
        return productIds.stream()
                .map(id -> {
                    Product p = productMap.get(id);// Instant lookup in our dictionary
                    if (p == null) return null; // Safety check (maybe product was deleted)

                    CartVO vo = new CartVO();
                    BeanUtils.copyProperties(p, vo); // Copy Name, Price, Image from MySQL Product
                    vo.setProductId(id);
                    vo.setQuantity(Integer.parseInt(entries.get(id.toString()).toString()));// Get Qty from Redis
                    return vo;
                })
                .filter(Objects::nonNull)// 3. QUALITY CONTROL: Throw away any 'null' items
                .collect(Collectors.toList());// 4. PACKAGING: Put the final VOs into a new List
    }

    public void clearCart(Long userId) {

        String key = "cart:user" + userId;
        stringRedisTemplate.unlink(key);
    }
}
