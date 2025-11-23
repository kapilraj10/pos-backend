package kapil.raj.pos.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderRequest {
    private String customerName;
    private String phoneNumber;
    private List<OrderItemRequest> cartItems;
    private Double subtotal;
    private double tax;
    private Double grandTotal;
    private  String paymentMethod;

    public Double getSubTotal() {
        return null;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static  class OrderItemRequest {
        private String name;
        private Double price;
        private Integer quantity;
    }

}
