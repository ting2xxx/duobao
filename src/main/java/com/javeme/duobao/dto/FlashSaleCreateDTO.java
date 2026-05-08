package com.javeme.duobao.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlashSaleCreateDTO {

    private Long productId;
    private BigDecimal flashSalePrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
