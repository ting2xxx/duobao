package com.javeme.duobao.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartVO {

    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
}
