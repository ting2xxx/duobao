package com.javeme.duobao.controller;

import com.javeme.duobao.dto.ProductDTO;
import com.javeme.duobao.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    /**
     * Publish flash sales
     * @param productId
     * @return
     */
    @PostMapping("/{id}/publish-flash-sales")
    public ResponseEntity<String> publishFlashSales(@PathVariable("id") Long productId) {
        adminProductService.publishFlashSales(productId);
        return ResponseEntity.ok("Flash Sale successfully published and cache pre-warmed!");
    }

    /**
     * End flash sales
     * @param productId
     * @return
     */
    @PostMapping("/{id}/end-flash-sales")
    public ResponseEntity<String> endFlashSales(@PathVariable("id") Long productId) {
        adminProductService.endFlashSales(productId);
        return ResponseEntity.ok("Flash Sale successfully ended!");
    }

    /**
     * Update stock
     * @param productId
     * @param quantity
     * @return
     */
    @PutMapping("/{productId}/stock")
    public ResponseEntity<String> updateStock(@PathVariable Long productId, @RequestParam Integer quantity) {
        adminProductService.updateStock(productId, quantity);
        return ResponseEntity.ok("Stock successfully updated!");
    }

    /**
     * Add product
     * @param productDTO
     * @return
     */
    @PostMapping("/add")
    public ResponseEntity<String> addProduct(@RequestBody ProductDTO productDTO) {
        adminProductService.addProduct(productDTO);
        return ResponseEntity.ok("Product successfully added!");
    }
}
