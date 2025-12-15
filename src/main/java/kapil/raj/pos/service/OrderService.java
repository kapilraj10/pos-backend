package kapil.raj.pos.service;

import kapil.raj.pos.entity.OrderEntity;
import kapil.raj.pos.io.OrderRequest;
import kapil.raj.pos.io.OrderResponse;

import java.awt.print.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderService {


    OrderResponse createOrder(OrderRequest request);


    void deleteOrder(String orderId);


    List<OrderResponse> getLatestOrders();


    Double sumSalesByDate(LocalDate date);
    Long countByOrderDate(LocalDate date);
    Optional<OrderEntity> findRecntOrders(Pageable pageable);

    List<OrderResponse> findRecentOrders();

    List<OrderResponse> findRecentOrders(Pageable pageable);
}
