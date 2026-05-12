package com.javeme.duobao.dto;

import lombok.Data;


import java.time.LocalDateTime;

@Data
public class GetOrderDto {

    private Integer status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String orderNumber;

    private Integer page = 1; //Default to page 1
    private Integer pageSize = 10;//Default to 10 items per page
}
