package com.javeme.duobao.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderVO {

    private Long id;
    private String orderNumber;
    private BigDecimal amount;
    private List<OrderItemVO> orderItems;
    private String userId;
    private String phone;
    private String addressBookId;
    private String consignee;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
    private LocalDateTime orderTime;
    private Integer deliveryStatus;
    private LocalDateTime deliveryTime;
    private String remark;
    private Integer status;

    private String firstItemName;
    private String firstItemImage;
}
