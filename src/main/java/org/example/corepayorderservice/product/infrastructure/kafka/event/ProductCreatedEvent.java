package org.example.corepayorderservice.product.infrastructure.kafka.event;

public record ProductCreatedEvent(
        Long productId,
        String name,
        int price,
        int discount
) {
}
