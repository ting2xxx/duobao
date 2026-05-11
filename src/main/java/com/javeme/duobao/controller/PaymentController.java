package com.javeme.duobao.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javeme.duobao.common.BaseContext;
import com.javeme.duobao.service.PaymentService;
import com.javeme.duobao.utils.WebhookSecurityUtil;
import com.javeme.duobao.vo.OrderVO;
import com.javeme.duobao.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import static com.javeme.duobao.utils.WebhookSecurityUtil.verifySignature;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Top up wallet
     * @param amount
     * @return
     */
    @PostMapping("/topup")
    public ResponseEntity<UserVO> topupWallet(@RequestParam BigDecimal amount) {
        Long currentID = BaseContext.getCurrentID();
        UserVO userVO = paymentService.topup(currentID, amount);
        return ResponseEntity.ok(userVO);
    }

    /**
     * Pay for order
     * @param orderNumber
     * @return
     */
    @PostMapping("/pay/{orderNumber}")
    public ResponseEntity<OrderVO> pay(@PathVariable String orderNumber) {
        OrderVO orderVO = paymentService.pay(orderNumber);
        return ResponseEntity.ok(orderVO);
    }

    /**
     * Handle payment webhook
     * @param signature
     * @param rawPayload
     * @return
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handlePaymentWebhook(
            @RequestHeader("Provider-Signature") String signature,
            @RequestBody String rawPayload) {

        //check signature whether is correct

        if (!WebhookSecurityUtil.verifySignature(rawPayload, signature)) {
            log.error("SECURITY ALERT: Invalid webhook signature detected!");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
        }

        try {
            //parse the payload
            ObjectMapper mapper = new ObjectMapper();
            JsonNode payload = mapper.readTree(rawPayload);

            String orderNumber = payload.get("orderNumber").asText();
            String status = payload.get("status").asText();

            if ("success".equals(status)) {

                paymentService.paySuccess(orderNumber);
            }

            return ResponseEntity.ok("SUCCESS");

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("FAIL");
        }
    }

    /**
     * Initiate payment
     * @param orderNumber
     * @return
     */
    @PostMapping("/initiate/{orderNumber}")
    public ResponseEntity<String> initiatePayment(@PathVariable String orderNumber) {

        Long userId = BaseContext.getCurrentID();

        String paymentUrl = paymentService.getPaymentLink(userId, orderNumber);

        return ResponseEntity.ok(paymentUrl);
    }
}
