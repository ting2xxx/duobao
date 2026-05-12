package com.javeme.duobao.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class WebhookSecurityUtil {

    public static final String SECRET_KEY = "my_secret_webhook_key";

    /**
     * This does the heavy math. It takes the raw JSON and the secret key,
     * and generates the HMAC-SHA256 signature.
     */
    public static String generateSignature(String payload, String secret) {

        try {

            Mac mac = Mac.getInstance("HmacSha256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            //Convert the byte array  into readable Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC signature", e);
        }
    }

    /**
     * The Bouncer: Compares the header signature with our own calculated signature
     */
    public static boolean verifySignature(String payload, String signatureFromHeader) {
        String expectedSignature  = generateSignature(payload, SECRET_KEY);
        // We use .equals to securely check if the strings match perfectly
        return expectedSignature.equals(signatureFromHeader);
    }
}
