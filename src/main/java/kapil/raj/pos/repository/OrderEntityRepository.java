package kapil.raj.pos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kapil.raj.pos.entity.OrderEntity;

public interface OrderEntityRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderId(String orderId);

    boolean existsByOrderId(String orderId);

    void deleteByOrderId(String orderId);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findTop10ByOrderByCreatedAtDesc();

    /* ✅ SUM sales by date (BEST PRACTICE) */
    @Query("SELECT SUM(o.grandTotal) FROM OrderEntity o " +
            "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate")
    Double sumSalesByDate(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /* ✅ COUNT orders by date */
    @Query("SELECT COUNT(o) FROM OrderEntity o " +
            "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate")
    Long countByOrderDate(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /* ✅ Recent orders with pagination */
    @Query("SELECT o FROM OrderEntity o ORDER BY o.createdAt DESC")
    List<OrderEntity> findRecentOrders(Pageable pageable);
}
