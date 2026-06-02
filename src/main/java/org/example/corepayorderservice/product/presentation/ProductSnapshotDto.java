package org.example.corepayorderservice.product.presentation;

import org.example.corepayorderservice.product.domain.ProductSnapshot;

public record ProductSnapshotDto(
        Long id,
        String name,
        int price,
        int discount
) {

    public static ProductSnapshotDto from(ProductSnapshot entity) {
        return new ProductSnapshotDto(
                entity.getId(),
                entity.getName(),
                entity.getPrice(),
                entity.getDiscount()
        );
    }
}
