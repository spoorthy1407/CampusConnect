package com.campusconnect.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Service
public class RazorpayService {
    @Value("${razorpay.key-id:}")
    private String keyId;
    @Value("${razorpay.key-secret:}")
    private String keySecret;

    private final RestClient http = RestClient.create();

    public boolean enabled() { return !keyId.isBlank() && !keySecret.isBlank(); }

    public String keyId() { return keyId; }

    public String createOrder(long amountPaise, String receipt) {
        try {
            Map<?, ?> res = http.post()
                    .uri("https://api.razorpay.com/v1/orders")
                    .headers(h -> h.setBasicAuth(keyId, keySecret))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amountPaise, "currency", "INR", "receipt", receipt))
                    .retrieve()
                    .body(Map.class);
            if (res == null || res.get("id") == null) throw new IllegalStateException("No order id");
            return String.valueOf(res.get("id"));
        } catch (RestClientException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not start the payment. Please try again from My Print Jobs.");
        }
    }

    public boolean verify(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
