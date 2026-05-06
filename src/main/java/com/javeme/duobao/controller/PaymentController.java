package com.javeme.duobao.controller;

import com.javeme.duobao.common.BaseContext;
import com.javeme.duobao.service.PaymentService;
import com.javeme.duobao.vo.OrderVO;
import com.javeme.duobao.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/topup")
    public ResponseEntity<UserVO> topupWallet(@RequestParam BigDecimal amount) {
        Long currentID = BaseContext.getCurrentID();
        UserVO userVO = paymentService.topup(currentID, amount);
        return ResponseEntity.ok(userVO);
    }

    @PostMapping("/pay/{orderNumber}")
    public ResponseEntity<OrderVO> pay(@PathVariable String orderNumber) {
        OrderVO orderVO = paymentService.pay(orderNumber);
        return ResponseEntity.ok(orderVO);
    }
}
