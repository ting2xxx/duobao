package com.javeme.duobao.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlashSaleResponseDTO {

    private Long flashSaleId;
    private Long productId;
    private String productName;
    private String image;
    private BigDecimal originalPrice;
    private BigDecimal flashSalePrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}
