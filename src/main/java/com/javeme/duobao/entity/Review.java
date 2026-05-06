package com.javeme.duobao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "review")
@SQLDelete(sql = "UPDATE review SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long productId;
    private Long orderItemId;
    private Integer rating;
    private String comment;
    private LocalDateTime createTime;
    private Boolean isDeleted = false;
}
