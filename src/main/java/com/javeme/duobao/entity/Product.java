package com.javeme.duobao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SQLDelete(sql = "UPDATE product SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productName;
    private BigDecimal price;
    private Integer stock;
    private Long categoryId;
    private String image;

    //0:下架 1:上架
    private Integer status;
    private String description;
    private LocalDateTime createTime;
    private Long createUser;
    private LocalDateTime updateTime;
    private Long updateUser;
    private Boolean isFlashSale;
    private Boolean isDeleted;
    private Double rating;
}
