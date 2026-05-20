package org.example.corepayorderservice.product.application;

import org.example.corepayorderservice.product.infrastructure.kafka.event.ProductCreatedEvent;
import org.example.corepayorderservice.product.presentation.ProductSnapshotDto;

import java.util.List;
import java.util.Map;

public interface ProductSnapshotService {

    Map<Long, ProductSnapshotDto> getProductInfos(List<Long> productIds);
    void productSnapshotSave(ProductCreatedEvent event);
}
