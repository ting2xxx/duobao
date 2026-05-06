package com.javeme.duobao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {

    private String username;
    private String avatar;
    private Integer gender;
    private LocalDate dateOfBirth;
}
