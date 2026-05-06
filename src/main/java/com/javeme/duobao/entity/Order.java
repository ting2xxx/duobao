package com.javeme.duobao.entity;

import com.javeme.duobao.vo.OrderItemVO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    /**
     * 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     */
    public static final Integer PENDING_PAYMENT = 1;
    public static final Integer TO_BE_CONFIRMED = 2;
    public static final Integer CONFIRMED = 3;
    public static final Integer DELIVERY_IN_PROGRESS = 4;
    public static final Integer COMPLETED = 5;
    public static final Integer CANCELLED = 6;

    /**
     * 支付状态 0未支付 1已支付 2退款
     */
    public static final Integer UN_PAID = 0;
    public static final Integer PAID = 1;
    public static final Integer REFUND = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer status;
    private String orderNumber;
    private Long addressBookId;
    private LocalDateTime orderTime;
    private Integer payStatus;
    private BigDecimal amount;
    private String remark;
    private Long userId;
    private String username;
    private String phone;
    private String address;
    private String postcode;
    private String consignee;

    @Transient
    private List<OrderItemVO> orderItems;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
    private Integer deliveryStatus;
    private LocalDateTime deliveryTime;
}
