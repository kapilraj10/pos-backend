package kapil.raj.pos.service.impl;

import kapil.raj.pos.entity.OrderEntity;
import kapil.raj.pos.entity.OrderItemEntity;
import kapil.raj.pos.io.OrderRequest;
import kapil.raj.pos.io.OrderResponse;
import kapil.raj.pos.io.PaymentDetails;
import kapil.raj.pos.io.PaymentMethod;
import kapil.raj.pos.repository.OrderEntityRepository;
import kapil.raj.pos.service.OrderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderEntityRepository orderEntityRepository;

    public OrderServiceImpl(OrderEntityRepository orderEntityRepository) {
        this.orderEntityRepository = orderEntityRepository;
    }

    @Override
    public OrderResponse createOrder(OrderRequest request) {

        // Convert request to OrderEntity
        OrderEntity newOrder = convertToOrderEntity(request);

        // Set payment details based on payment method
        PaymentDetails payment = new PaymentDetails();
        payment.setStatus(
                newOrder.getPaymentMethod() == PaymentMethod.CASH
                        ? PaymentDetails.PaymentStatus.COMPLETED
                        : PaymentDetails.PaymentStatus.PENDING
        );
        newOrder.setPaymentDetails(payment);

        // Convert cart items
        List<OrderItemEntity> orderItems = request.getCartItems().stream()
                .map(this::convertToOrderItem)
                .collect(Collectors.toList());
        newOrder.setItems(orderItems);

        // Save order to database
        newOrder = orderEntityRepository.save(newOrder);

        // Convert saved order to response DTO
        return convertToResponse(newOrder);
    }

    private OrderEntity convertToOrderEntity(OrderRequest request) {
        // Convert string to PaymentMethod enum safely
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            paymentMethod = PaymentMethod.CASH; // default if invalid or null
        }

        return OrderEntity.builder()
                .customerName(request.getCustomerName())
                .phoneNumber(request.getPhoneNumber())
                .subTotal(request.getSubTotal())
                .tax(request.getTax())
                .grandTotal(request.getGrandTotal())
                .paymentMethod(paymentMethod)
                .build();
    }

    private OrderItemEntity convertToOrderItem(OrderRequest.OrderItemRequest cartItem) {
        return OrderItemEntity.builder()
                .name(cartItem.getName())
                .quantity(cartItem.getQuantity())
                .price(cartItem.getPrice())
                .build();
    }

    private OrderResponse convertToResponse(OrderEntity order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .customerName(order.getCustomerName())
                .phoneNumber(order.getPhoneNumber())
                .subtotal(order.getSubTotal())
                .tax(order.getTax())
                .grandTotal(order.getGrandTotal())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null) // FIXED
                .paymentDetails(order.getPaymentDetails())
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Override
    public void deleteOrder(String orderId) {
        // Delete by order entity ID (assuming it's numeric)
        orderEntityRepository.deleteById(Long.parseLong(orderId));
    }

    @Override
    public List<OrderResponse> getLatestOrders() {
        return orderEntityRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
}
