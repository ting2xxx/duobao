package com.javeme.duobao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String phone;
    //1=Male, 2=Female
    private Integer gender;
    private String avatar;
    //0=Banned, 1=Active
    private Integer status;
    private Integer age;
    //0=Admin, 1=Customer
    private Integer role;
    private BigDecimal balance;
    private LocalDate dateOfBirth;
    private LocalDateTime createDate;
    private Long createUser;
    private LocalDateTime updateDate;
    private Long updateUser;
}
