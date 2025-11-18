package kapil.raj.pos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import kapil.raj.pos.entity.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long>{

    Optional<CategoryEntity> findByCategoryId(String categoryId);
}