package com.javeme.duobao.controller;

import com.javeme.duobao.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    @PostMapping("/{id}/publish-flash-sales")
    public ResponseEntity<String> publishFlashSales(@PathVariable("id") Long productId) {
        adminProductService.publishFlashSales(productId);
        return ResponseEntity.ok("Flash Sale successfully published and cache pre-warmed!");
    }

    @PostMapping("/{id}/end-flash-sales")
    public ResponseEntity<String> endFlashSales(@PathVariable("id") Long productId) {
        adminProductService.endFlashSales(productId);
        return ResponseEntity.ok("Flash Sale successfully ended!");
    }

    @PutMapping("/{productId}/stock")
    public ResponseEntity<String> updateStock(@PathVariable Long productId, @RequestParam Integer quantity) {
        adminProductService.updateStock(productId, quantity);
        return ResponseEntity.ok("Stock successfully updated!");
    }
}
