package kapil.raj.pos.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import kapil.raj.pos.entity.OrderEntity;
import kapil.raj.pos.entity.OrderItemEntity;
import kapil.raj.pos.io.OrderRequest;
import kapil.raj.pos.io.OrderResponse;
import kapil.raj.pos.io.PaymentDetails;
import kapil.raj.pos.io.PaymentMethod;
import kapil.raj.pos.repository.OrderEntityRepository;
import kapil.raj.pos.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderEntityRepository orderEntityRepository;

    public OrderServiceImpl(OrderEntityRepository orderEntityRepository) {
        this.orderEntityRepository = orderEntityRepository;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body cannot be null");
        }

        if (request.getCartItems() == null || request.getCartItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart items cannot be empty");
        }

        PaymentMethod paymentMethod = parsePaymentMethod(request.getPaymentMethod());

        // Compute subtotal if not provided
        double computedSubtotal = request.getCartItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        Double subTotal = request.getSubTotal() != null ? request.getSubTotal() : computedSubtotal;
        Double tax = request.getTax() != null ? request.getTax() : 0.0;
        Double grandTotal = request.getGrandTotal() != null ? request.getGrandTotal() : subTotal + tax;

        // Create order entity
        OrderEntity order = OrderEntity.builder()
                .customerName(request.getCustomerName())
                .phoneNumber(request.getPhoneNumber())
                .subTotal(subTotal)
                .tax(tax)
                .grandTotal(grandTotal)
                .paymentMethod(paymentMethod)
                .build();

        // Payment details
        PaymentDetails payment = new PaymentDetails();
        payment.setStatus(paymentMethod == PaymentMethod.CASH
                ? PaymentDetails.PaymentStatus.COMPLETED
                : PaymentDetails.PaymentStatus.PENDING);
        order.setPaymentDetails(payment);

        // Map order items
        List<OrderItemEntity> orderItems = request.getCartItems().stream()
                .map(this::convertToOrderItem)
                .collect(Collectors.toList());
        order.setItems(orderItems);

        order = orderEntityRepository.save(order);

        return convertToResponse(order);
    }

    @Override
    @Transactional
    public void deleteOrder(String orderId) {
        Long id = parseId(orderId);
        if (!orderEntityRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order with id " + orderId + " not found");
        }
        orderEntityRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getLatestOrders() {
        return orderEntityRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private PaymentMethod parsePaymentMethod(String raw) {
        if (raw == null || raw.isBlank()) {
            return PaymentMethod.CASH;
        }
        try {
            return PaymentMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment method: " + raw);
        }
    }

    private OrderItemEntity convertToOrderItem(OrderRequest.OrderItemRequest cartItem) {
        if (cartItem.getQuantity() <= 0 || cartItem.getPrice() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid item quantity or price for item: " + cartItem.getName());
        }
        return OrderItemEntity.builder()
                .name(cartItem.getName())
                .quantity(cartItem.getQuantity())
                .price(cartItem.getPrice())
                .build();
    }

    private OrderResponse convertToResponse(OrderEntity order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                .filter(Objects::nonNull)
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
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .paymentDetails(order.getPaymentDetails())
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order ID: " + raw);
        }
    }
}
