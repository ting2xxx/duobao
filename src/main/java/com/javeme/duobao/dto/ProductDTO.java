package com.javeme.duobao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private String productName;
    private BigDecimal price;
    private Integer stock;
    private Long categoryId;
    private String image;
    private Integer status;
    private String description;
}
