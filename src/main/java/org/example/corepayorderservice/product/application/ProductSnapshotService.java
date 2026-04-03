package org.example.corepayorderservice.product.application;

import org.example.corepayorderservice.product.infrastructure.kafka.event.ProductCreatedEvent;
import org.example.corepayorderservice.product.presentation.ProductSnapshotDto;

public interface ProductSnapshotService {

    ProductSnapshotDto getProductInfo(Long productId);
    void productSnapshotSave(ProductCreatedEvent event);
}
