package com.javeme.duobao.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mock-gateway")
public class MockGatewayController {

    //must match the secret used in WebhookSecurityUtil
    private static final String WEBHOOK_SECRET = "my_secret_webhook_key";

    /**
     * Simulate the user clicking "Pay" on the external gateway
     * This will automatically trigger the webhook
     * @param orderNumber
     * @return
     */
    @PostMapping("/submit-payment")
    public ResponseEntity<String> submitFakePayment(@RequestParam String orderNumber) {

        //1.Create the payload webhook expected
        // Remove the space after the comma to make it a tight JSON string
        String payload = "{\"orderNumber\":\"" + orderNumber + "\",\"status\":\"success\"}";

        //2.Generate the provider signature
        String signature = generateHmacSha256(payload, WEBHOOK_SECRET);

        //3.Fire the webhook via HTTP (Simulating Stripe/PayPal calling the app)
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Provider-Signature", signature);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        String myWebhookUrl = "http://localhost:8080/api/payments/webhook";

        try {

            //Send the POST request to own webhook
            ResponseEntity<String> response = restTemplate.postForEntity(myWebhookUrl, request, String.class);
            return ResponseEntity.ok("FAKE GATEWAY: Payment processed. Webhook fired! Webhook response: " + response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("FAKE GATEWAY: Webhook failed to fire: " + e.getMessage());
        }

    }

    /**
     * Helper method to generate the signature (mimics what Stripe's servers do)
     * @param data
     * @param secret
     * @return
     */
    private String generateHmacSha256(String data, String secret) {

        try {

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {

            throw new RuntimeException("Failed to generate signature", e);
        }
    }

}
