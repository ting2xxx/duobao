package com.javeme.duobao.service;

import com.javeme.duobao.dto.UserLoginDTO;
import com.javeme.duobao.dto.UserRegisterDTO;
import com.javeme.duobao.entity.User;
import com.javeme.duobao.repository.UserRepository;
import com.javeme.duobao.utils.JwtUtil;
import com.javeme.duobao.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        //check username exist
        User user = userRepository.findByUsername(userLoginDTO.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        //generate hashed password
        String hashedPassword = DigestUtils.md5DigestAsHex(userLoginDTO.getPassword().getBytes());
        //check password
        if (!user.getPassword().equals(hashedPassword)) {
            throw new RuntimeException("Invalid password");
        }

        //check status
        if (user.getStatus() == 0) {
            throw new RuntimeException("User is banned");
        }
        //generate jwtToken with userId
        String token = JwtUtil.createToken(user.getId());

        //return userLoginVO
        UserLoginVO userLoginVO  =
                UserLoginVO.builder().
                        id(user.getId()).
                        username(user.getUsername()).
                        token(token).
                        build();

        return userLoginVO;
    }

    public void register(UserRegisterDTO userRegisterDTO) {

        //check username exist
        if (userRepository.findByUsername(userRegisterDTO.getUsername()) != null) {
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setUsername(userRegisterDTO.getUsername());
        String hashedPassword = DigestUtils.md5DigestAsHex(userRegisterDTO.getPassword().getBytes());
        user.setPassword(hashedPassword);
        user.setCreateDate(LocalDateTime.now());
        user.setStatus(1);
        user.setRole(1);
        userRepository.save(user);
    }
}
