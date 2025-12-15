package kapil.raj.pos.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class KhaltiService {

    @Value("${khalti.api.base-url:https://dev.khalti.com/api/v2}")
    private String khaltiBaseUrl;

    @Value("${khalti.secret.key:}")
    private String khaltiSecretKey;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> initiatePayment(String returnUrl, String websiteUrl, Integer amountPaisa, String purchaseOrderId, String purchaseOrderName, Map<String, Object> customerInfo) {
        String url = khaltiBaseUrl + "/epayment/initiate/";

        Map<String, Object> body = new HashMap<>();
        body.put("return_url", returnUrl);
        body.put("website_url", websiteUrl);
        body.put("amount", amountPaisa);
        body.put("purchase_order_id", purchaseOrderId);
        body.put("purchase_order_name", purchaseOrderName);
        if (customerInfo != null) body.put("customer_info", customerInfo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (khaltiSecretKey != null && !khaltiSecretKey.isBlank()) {
            headers.set("Authorization", "Key " + khaltiSecretKey);
        } else {
            throw new RuntimeException("Khalti secret key is not configured");
        }

        System.out.println("🔹 Khalti Initiate Request:");
        System.out.println("   URL: " + url);
        System.out.println("   Body: " + body);
        System.out.println("   Secret Key Present: " + (khaltiSecretKey != null && !khaltiSecretKey.isBlank()));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> resp = rest.exchange(url, HttpMethod.POST, request, String.class);
            System.out.println("✅ Khalti Response Status: " + resp.getStatusCode());
            System.out.println("   Response Body: " + resp.getBody());
            
            JsonNode node = mapper.readTree(resp.getBody());
            return mapper.convertValue(node, Map.class);
        } catch (Exception ex) {
            System.err.println("❌ Khalti API Error: " + ex.getMessage());
            if (ex instanceof org.springframework.web.client.HttpStatusCodeException) {
                org.springframework.web.client.HttpStatusCodeException httpEx = (org.springframework.web.client.HttpStatusCodeException) ex;
                System.err.println("   Status: " + httpEx.getStatusCode());
                System.err.println("   Response: " + httpEx.getResponseBodyAsString());
            }
            throw new RuntimeException("Failed to initiate Khalti payment: " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> lookup(String pidx) {
        String url = khaltiBaseUrl + "/epayment/lookup/";

        Map<String, Object> body = Map.of("pidx", pidx);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (khaltiSecretKey != null && !khaltiSecretKey.isBlank()) {
            headers.set("Authorization", "Key " + khaltiSecretKey);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.POST, request, String.class);
        try {
            JsonNode node = mapper.readTree(resp.getBody());
            return mapper.convertValue(node, Map.class);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse Khalti lookup response", ex);
        }
    }
}
