package com.javeme.duobao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {

    private Long addressBookId;
    private String remark;
    private Long productId;
    private Integer quantity;

}
