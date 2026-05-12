package com.javeme.duobao.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailVO {

    private String orderNumber;
    private Integer status;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
    private LocalDateTime payTime;

    private OrderAddressVO shippingAddress;

    private List<OrderItemVO> items;
}
