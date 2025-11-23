package kapil.raj.pos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kapil.raj.pos.entity.OrderEntity;

public interface OrderEntityRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderId(Long orderId);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findTop10ByOrderByCreatedAtDesc();
}
