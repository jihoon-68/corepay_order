package org.example.corepayorderservice.product.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.product.application.ProductSnapshotService;
import org.example.corepayorderservice.product.infrastructure.kafka.event.ProductCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProductSnapshotService productSnapshotService;

    @KafkaListener(topics = "product-created-topic", groupId = "order-group")
    public void consumeProductCreated(String message) {
        try {
            ProductCreatedEvent event = objectMapper.readValue(message, ProductCreatedEvent.class);
            productSnapshotService.productSnapshotSave(event);
        } catch (JsonProcessingException e) {
            log.error(" 상품 이벤트 파싱 실패: {}", message, e);
        }
    }
}
