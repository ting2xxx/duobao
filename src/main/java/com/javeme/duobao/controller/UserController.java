package com.javeme.duobao.controller;

import com.javeme.duobao.dto.UserLoginDTO;
import com.javeme.duobao.dto.UserRegisterDTO;
import com.javeme.duobao.service.UserService;
import com.javeme.duobao.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        UserLoginVO userLoginVO = userService.login(userLoginDTO);
        return ResponseEntity.ok(userLoginVO);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        userService.register(userRegisterDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
