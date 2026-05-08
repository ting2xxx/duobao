package com.javeme.duobao.controller;

import com.javeme.duobao.dto.FlashSaleCreateDTO;
import com.javeme.duobao.dto.FlashSaleResponseDTO;
import com.javeme.duobao.entity.FlashSale;
import com.javeme.duobao.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flash-sales")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping("/calendar")
    private ResponseEntity<Map<String,List<FlashSaleResponseDTO>>> getCalendar(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Map<String,List<FlashSaleResponseDTO>> flashSale = flashSaleService.getCalendar(startTime, endTime);
        return ResponseEntity.ok(flashSale);
    }

    @PostMapping("/add")
    public ResponseEntity<String> addFlashSale(@RequestBody FlashSaleCreateDTO flashSaleCreateDTO) {
        flashSaleService.createFlashSale(flashSaleCreateDTO);
        return ResponseEntity.ok("Flash sale successfully added to the calendar");
    }
}
