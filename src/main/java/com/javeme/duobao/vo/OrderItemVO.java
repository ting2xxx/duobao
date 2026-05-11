package com.javeme.duobao.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemVO {

    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private BigDecimal amount;

}
