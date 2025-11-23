package kapil.raj.pos.entity;

import jakarta.persistence.*;
import kapil.raj.pos.io.PaymentMethod;
import kapil.raj.pos.io.PaymentDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tbl_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private String customerName;
    private String phoneNumber;
    private Double subTotal;
    private Double tax;
    private Double grandTotal;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Transient
    private PaymentDetails paymentDetails;

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItemEntity> items;

    private LocalDateTime createdAt = LocalDateTime.now();
}
