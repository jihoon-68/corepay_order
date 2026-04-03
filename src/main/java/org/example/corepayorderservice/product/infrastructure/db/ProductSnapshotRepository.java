package org.example.corepayorderservice.product.infrastructure.db;

import org.example.corepayorderservice.product.domain.ProductSnapshot;
import org.example.corepayorderservice.product.presentation.ProductSnapshotDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ProductSnapshotRepository extends JpaRepository<ProductSnapshot, Long > {

    Optional<ProductSnapshotDto> findByProductId(Long productId);
}
