package com.javeme.duobao.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressBookDTO {

    private Long id;

    private String consignee;
    private String phone;
    private Integer gender;

    private String province;
    private String city;
    private String address;
    private String postcode;

    private String label;
    private Integer isDefault;
}
