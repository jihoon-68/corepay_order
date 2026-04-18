package org.example.corepayorderservice.order.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.StockIncreaseEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendOrderCreated(OrderCreatedEvent event){
        log.info("주문 이벤트 발행: {}", event);
        sendMessage("order-created-topic", event);
    }

    public void sendStockIncrease(StockIncreaseEvent event){
        log.info("재고 복구 이벤트 발행: {}", event);
        sendMessage("stock-increase-topic", event);
    }

    public void sendPaymentCancel(PaymentCancelEvent event){
        log.info("재고 취소 이벤트 발행: {}", event);
        sendMessage("payment-cancel-topic", event);
    }


    private void sendMessage(String topic, Object event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, message);
            log.info("[카프카 발송 성공] 토픽: {}, 메시지: {}", topic, message);
        } catch (JsonProcessingException e) {
            log.error("카프카 메시지 직렬화 에러. 토픽: {}", topic, e);
        }
    }
}
