package com.javeme.duobao.service;

import com.javeme.duobao.dto.CartItemDTO;
import com.javeme.duobao.entity.Product;
import com.javeme.duobao.repository.ProductRepository;
import com.javeme.duobao.vo.CartVO;
import lombok.RequiredArgsConstructor;
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

        //generate a key for cart, each cart pairs with one user
        String key = "cart:user" + userId;
        String productId = cartItemDTO.getProductId().toString();

        //get product with productId
        Product product = productRepository.findById(cartItemDTO.getProductId()).orElseThrow(() ->
                new RuntimeException("Product not found"));

        //get cart item from redis
        Object currentObj = stringRedisTemplate.opsForHash().get(key, productId);

        //if cart is null, currentInCart = 0, if not, currentInCart = currentObj item count
        int currentInCart = (currentObj == null) ? 0 : Integer.parseInt(currentObj.toString());

        //Get total amount of currentInCart + cartItemDTO.getQuantity()
        int total = currentInCart + cartItemDTO.getQuantity();

        //if the total item is greater than the stock, throw exception
        if (total > product.getStock()) {

            throw new RuntimeException("Limit reached! You already have " + currentInCart + " items in your cart");
        }

        // if total is 0 or less, remove this specific item from the user's cart
        if (total <= 0) {
            stringRedisTemplate.opsForHash().delete(key, productId);

        } else {
            //if not, add item into redis
            stringRedisTemplate.opsForHash().put(key, productId, String.valueOf(total));
        }
    }

    public List<CartVO> getCart(Long userId) {
        String key = "cart:user" + userId;

        // 1. Get productId and quantity from the Redis Hash (Map<ProductId, Quantity>)
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);

        //if there is nothing, return empty list
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Extract IDs and fetch all products from DB in ONE query. using entries.keySet
        List<Long> productIds = entries.keySet().stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());

        List<Product> products = productRepository.findAllById(productIds);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

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
