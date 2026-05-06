package com.javeme.duobao.controller;

import com.javeme.duobao.dto.CartItemDTO;
import com.javeme.duobao.service.CartService;
import com.javeme.duobao.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
public class CartController {

    private final StringRedisTemplate stringRedisTemplate;
    private final CartService cartService;
    @PostMapping("/add")
    public ResponseEntity<String> add(@RequestHeader("userId") Long userId,
                                      @RequestBody CartItemDTO cartItemDTO) {

        cartService.addCart(userId, cartItemDTO);
        return ResponseEntity.ok("Cart successfully updated!");
    }

    @GetMapping
    public ResponseEntity<List<CartVO>> getCart(@RequestHeader("userId") Long userId) {
        List<CartVO> cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clear(@RequestHeader("userId") Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
