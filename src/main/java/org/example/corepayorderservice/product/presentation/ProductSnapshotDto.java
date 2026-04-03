package org.example.corepayorderservice.product.presentation;

import org.example.corepayorderservice.product.domain.ProductSnapshot;

public record ProductSnapshotDto(
        Long productId,
        String name,
        int price,
        int discount
) {

    public static ProductSnapshotDto from(ProductSnapshot entity) {
        return new ProductSnapshotDto(
                entity.getProductId(),
                entity.getName(),
                entity.getPrice(),
                entity.getDiscount()
        );
    }
}
