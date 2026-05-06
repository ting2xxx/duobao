package com.javeme.duobao.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO {

    private Long id;
    private String productName;
    private BigDecimal price;
    private Integer stock;
    private String image;
    private String description;
}
