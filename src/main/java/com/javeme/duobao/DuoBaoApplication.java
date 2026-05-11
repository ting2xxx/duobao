package com.javeme.duobao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static com.javeme.duobao.utils.WebhookSecurityUtil.*;

@EnableScheduling
@SpringBootApplication
public class DuoBaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DuoBaoApplication.class, args);

        String rawJson = "{\"orderNumber\":\"dc0be7b4-6fa3-405c-a893-9e8cfcaed104\",\"status\":\"success\"}";

        // 2. The Mock Provider generates the signature using the secret key
        String signatureToSend = generateSignature(rawJson, SECRET_KEY);

        System.out.println("--- WHAT THE PROVIDER SENDS ---");
        System.out.println("Header [Provider-Signature]: " + signatureToSend);
        System.out.println("Body:\n" + rawJson);

        System.out.println("\n--- WHAT YOUR SERVER DOES ---");
        // 3. Your server receives it and runs the Bouncer check
        boolean isSafe = verifySignature(rawJson, signatureToSend);
        System.out.println("Is the webhook safe? " + isSafe);

        // 4. Try to hack it! (Change the order number slightly)
        String hackedJson = "{\n  \"orderNumber\": \"ORD-999\",\n  \"status\": \"success\"\n}";
        boolean isHackedSafe = verifySignature(hackedJson, signatureToSend);
        System.out.println("Is the hacked webhook safe? " + isHackedSafe);
    }


}
