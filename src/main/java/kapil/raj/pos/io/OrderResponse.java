package kapil.raj.pos.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private String orderId;
    private String customerName;
    private String phoneNumber;
    private List<OrderItemResponse> items;
    private Double subtotal;
    private double tax;
    private Double grandTotal;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private PaymentDetails paymentDetails;


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemResponse {
        private String name;
        private Double price;
        private Integer quantity;
        private Long id;
    }
}
