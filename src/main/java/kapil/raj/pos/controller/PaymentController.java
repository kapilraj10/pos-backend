package kapil.raj.pos.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import kapil.raj.pos.io.OrderRequest;
import kapil.raj.pos.io.OrderResponse;
import kapil.raj.pos.service.KhaltiService;
import kapil.raj.pos.service.OrderService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final OrderService orderService;
    private final KhaltiService khaltiService;

    // Test endpoint to verify authentication is working
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> testAuth() {
        System.out.println("✅ Payment test endpoint reached - Auth is working!");
        return Map.of("status", "success", "message", "Authentication working");
    }

    // Initiate a payment: create an order and call Khalti initiate API
    @PostMapping("/initiate")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> initiatePayment(@RequestBody Map<String, Object> payload) {
        System.out.println("🔹 Payment initiate endpoint called");
        System.out.println("   Payload: " + payload);
        
        // Expected payload: { order: {...OrderRequest...}, return_url: "..", website_url: ".." }
        Map<String, Object> orderMap = (Map<String, Object>) payload.get("order");
        String returnUrl = (String) payload.getOrDefault("return_url", payload.get("returnUrl"));
        String websiteUrl = (String) payload.getOrDefault("website_url", payload.get("websiteUrl"));

        // Map to OrderRequest via Spring conversion by calling OrderService.createOrder using the fields
        OrderRequest orderReq = new OrderRequest();
        if (orderMap != null) {
            orderReq.setCustomerName((String) orderMap.get("customerName"));
            orderReq.setPhoneNumber((String) orderMap.get("phoneNumber"));
            orderReq.setSubTotal(orderMap.get("subTotal") instanceof Number ? ((Number) orderMap.get("subTotal")).doubleValue() : null);
            orderReq.setTax(orderMap.get("tax") instanceof Number ? ((Number) orderMap.get("tax")).doubleValue() : null);
            orderReq.setGrandTotal(orderMap.get("grandTotal") instanceof Number ? ((Number) orderMap.get("grandTotal")).doubleValue() : null);
            orderReq.setPaymentMethod((String) orderMap.getOrDefault("paymentMethod", "KHALTI"));
            
            // Convert cart items from List of Maps to List of OrderItemRequest
            java.util.List<?> cartItemsList = (java.util.List<?>) orderMap.get("cartItems");
            if (cartItemsList != null) {
                java.util.List<OrderRequest.OrderItemRequest> orderItems = cartItemsList.stream()
                    .map(item -> {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        OrderRequest.OrderItemRequest orderItem = new OrderRequest.OrderItemRequest();
                        orderItem.setName((String) itemMap.get("name"));
                        orderItem.setQuantity(itemMap.get("quantity") instanceof Number ? ((Number) itemMap.get("quantity")).intValue() : null);
                        orderItem.setPrice(itemMap.get("price") instanceof Number ? ((Number) itemMap.get("price")).doubleValue() : null);
                        return orderItem;
                    })
                    .collect(java.util.stream.Collectors.toList());
                orderReq.setCartItems(orderItems);
            }
        }

        // Create order in system
        OrderResponse created = orderService.createOrder(orderReq);

        // Prepare Khalti initiate request
        int amountPaisa = (int) Math.round((created.getGrandTotal() == null ? 0.0 : created.getGrandTotal()) * 100);
        String purchaseOrderId = created.getOrderId();
        String purchaseOrderName = "Order " + purchaseOrderId;

        Map<String, Object> customerInfo = Map.of(
                "name", created.getCustomerName(),
                "phone", created.getPhoneNumber()
        );

        Map<String, Object> khaltiResp = khaltiService.initiatePayment(returnUrl != null ? returnUrl : "", websiteUrl != null ? websiteUrl : "", amountPaisa, purchaseOrderId, purchaseOrderName, customerInfo);

        // Return both order info and khalti response to frontend
        return Map.of(
                "order", created,
                "khalti", khaltiResp
        );
    }

    @PostMapping("/lookup")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> lookup(@RequestBody Map<String, String> payload) {
        String pidx = payload.get("pidx");
        if (pidx == null || pidx.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "pidx required");
        }

        return khaltiService.lookup(pidx);
    }
}
