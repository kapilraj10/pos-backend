package kapil.raj.pos.service.impl;

import java.awt.print.Pageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import kapil.raj.pos.repository.ItemRepository;
import kapil.raj.pos.entity.ItemEntity;
import kapil.raj.pos.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderEntityRepository orderEntityRepository;

    private final ItemRepository itemRepository;

    public OrderServiceImpl(OrderEntityRepository orderEntityRepository, ItemRepository itemRepository) {
        this.orderEntityRepository = orderEntityRepository;
        this.itemRepository = itemRepository;
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

        double computedSubtotal = request.getCartItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        Double subTotal = request.getSubTotal() != null ? request.getSubTotal() : computedSubtotal;
        Double tax = request.getTax() != null ? request.getTax() : 0.0;
        Double grandTotal = request.getGrandTotal() != null ? request.getGrandTotal() : subTotal + tax;

        // CREATE ORDER ENTITY
        OrderEntity order = OrderEntity.builder()
                .orderId(generateOrderId())
                .customerName(request.getCustomerName())
                .phoneNumber(request.getPhoneNumber())
                .subTotal(subTotal)
                .tax(tax)
                .grandTotal(grandTotal)
                .paymentMethod(paymentMethod)
                .build();

        // PAYMENT DETAILS
        PaymentDetails payment = new PaymentDetails();
        payment.setStatus(paymentMethod == PaymentMethod.CASH
                ? PaymentDetails.PaymentStatus.COMPLETED
                : PaymentDetails.PaymentStatus.PENDING);
        order.setPaymentDetails(payment);

        // MAP ITEMS - convert cart items to entities
        List<OrderItemEntity> orderItems = request.getCartItems().stream()
                .map(this::convertToOrderItem)
                .collect(Collectors.toList());
        
        // Set bidirectional relationship - items need to know their parent order
        for (OrderItemEntity item : orderItems) {
            item.setOrder(order);
        }
        order.setItems(orderItems);

        // Save order with cascaded items
        order = orderEntityRepository.save(order);
        return convertToResponse(order);
    }

    //  DELETE ORDER
    @Override
    @Transactional
    public void deleteOrder(String orderId) {
        Long id = parseId(orderId);
        if (!orderEntityRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order with id " + orderId + " not found");
        }
        orderEntityRepository.deleteById(id);
    }

    // GET LATEST ORDERS
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getLatestOrders() {
        return orderEntityRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Double sumSalesByDate(LocalDate date) {
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.plusDays(1).atStartOfDay();
        return orderEntityRepository.sumSalesByDate(startDate, endDate);
    }

    @Override
    public Long countByOrderDate(LocalDate date) {
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.plusDays(1).atStartOfDay();
        return orderEntityRepository.countByOrderDate(startDate, endDate);
    }

    @Override
    public Optional<OrderEntity> findRecntOrders(Pageable pageable) {
        return Optional.empty();
    }

    @Override
    public List<OrderResponse> findRecentOrders() {
        // Return the top 10 most recent orders
        return orderEntityRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> findRecentOrders(Pageable pageable) {
        return orderEntityRepository.findRecentOrders((org.springframework.data.domain.Pageable) pageable)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }


    // CONVERT STRING TO ENUM
    private PaymentMethod parsePaymentMethod(String raw) {
        if (raw == null || raw.isBlank()) return PaymentMethod.CASH;
        try {
            return PaymentMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment method: " + raw);
        }
    }

    // CONVERT CART ITEM TO ENTITY
    private OrderItemEntity convertToOrderItem(OrderRequest.OrderItemRequest c) {
        if (c.getQuantity() <= 0 || c.getPrice() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid quantity or price for item: " + c.getName());
        }
        // If itemId is provided, decrement stock in DB
        if (c.getItemId() != null && !c.getItemId().isBlank()) {
            ItemEntity item = itemRepository.findByItemId(c.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item not found: " + c.getItemId()));
            int current = item.getStock() == null ? 0 : item.getStock();
            // If this item is low in stock, only allow one unit per order/reservation
            if (current <= 5 && c.getQuantity() > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Item '" + c.getName() + "' is low in stock (" + current + ") - max 1 unit allowed");
            }
            if (current < c.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for item: " + c.getName());
            }
            item.setStock(current - c.getQuantity());
            itemRepository.save(item);
        }

        return OrderItemEntity.builder()
                .itemId(c.getItemId())
                .name(c.getName())
                .quantity(c.getQuantity())
                .price(c.getPrice())
                .build();
    }

    // CONVERT ENTITY TO RESPONSE
    private OrderResponse convertToResponse(OrderEntity order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream().filter(Objects::nonNull)
        .map(i -> OrderResponse.OrderItemResponse.builder()
            .id(i.getId())
            .itemId(i.getItemId())
            .name(i.getName())
            .quantity(i.getQuantity())
            .price(i.getPrice())
            .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .customerName(order.getCustomerName())
                .phoneNumber(order.getPhoneNumber())
                .subtotal(order.getSubTotal())
                .tax(order.getTax())
                .grandTotal(order.getGrandTotal())
                .paymentMethod(order.getPaymentMethod().name())
                .paymentDetails(order.getPaymentDetails())
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }

    // PARSE TO LONG
    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order ID: " + raw);
        }
    }

    // AUTO GENERATE ORDER ID
    private String generateOrderId() {
        return "ORD" + System.currentTimeMillis();
    }
}
