package kapil.raj.pos.repository;

import kapil.raj.pos.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<ItemEntity, Long> {

    Optional<ItemEntity> findByItemId(String itemId);

    // Corrected count method using nested property
    Integer countByCategory_CategoryId(String categoryId);
}
