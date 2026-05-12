package com.javeme.duobao.vo;

import lombok.Data;

@Data
public class OrderAddressVO {

    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String detailAddress;
}
