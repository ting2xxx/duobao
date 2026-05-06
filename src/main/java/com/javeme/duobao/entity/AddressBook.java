package com.javeme.duobao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "address_book")
public class AddressBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String consignee; // Name on the package
    private String phone;
    private Integer gender;   // 1=Male, 2=Female

    // Address Details
    private String province;  // Added! (e.g., "California" or "Selangor")
    private String city;
    private String address;   // Street and apartment number
    private String postcode;

    // UX Features
    private String label;     // "Home", "Company", "School"
    private Integer isDefault;
}
