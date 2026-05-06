package com.javeme.duobao.dto;

import lombok.Data;

@Data
public class ReviewDTO {

    private Long productId;
    private Long orderItemId;
    private Integer rating;
    private String comment;
}
