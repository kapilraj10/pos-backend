package kapil.raj.pos.service;

import kapil.raj.pos.io.OrderRequest;
import kapil.raj.pos.io.OrderResponse;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    void deleteOrder(String orderId);
    List<OrderResponse> getLatestOrders();
}
