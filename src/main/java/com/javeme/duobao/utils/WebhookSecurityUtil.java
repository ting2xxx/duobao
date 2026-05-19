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
            //use MAC to get specific algorithm called HmacSHa256
            Mac mac = Mac.getInstance("HmacSha256");
            //convert the secret into raw UTF-8 bytes to create the Key
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            //insert the key into the HmacSHA256, turning it on
            mac.init(secretKeySpec);

            //Convert the payload into byte[]
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            //Convert the byte array into readable Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                //0xff & b force the byte act like a positive number
                //Integer.toHexString translates the raw byte into hex (0-9) (a-f)
                String hex = Integer.toHexString(0xff & b);
                //In hex, byte must be 2 character long, if translation result just a 5, add 0 in front, make it 05
                if (hex.length() == 1) hexString.append('0');
                //add it into one string
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
