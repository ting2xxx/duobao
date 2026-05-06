package com.javeme.duobao.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String phone; // In a real app, you might mask this: "123****890"
    private Integer gender;
    private String avatar;
    private Integer age;
    private Integer role; // Frontend might need this to show/hide an "Admin Panel" button
    private BigDecimal balance;
    private LocalDate dateOfBirth;
}
