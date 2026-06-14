package org.example.corepayorderservice.order.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderCancelledEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.StockConfirmEvent;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendPaymentCancel(PaymentCancelEvent event){
        log.info("결제 취소 이벤트 발행: {}", event);
        sendMessage("payment-cancel-topic", event);
    }

    // ★ 신규: 결제 성공 → DB 재고 확정
    public void sendStockConfirm(StockConfirmEvent event) {
        log.info("재고 확정 이벤트 발행: {}", event);
        sendMessage("stock-confirm-topic", event);
    }

    // ★ 신규: 결제 실패 → Redis 재고 복구
    public void sendOrderCancelled(OrderCancelledEvent event) {
        log.info("주문 취소(재고 복구) 이벤트 발행: {}", event);
        sendMessage("order-cancelled-topic", event);
    }


    private void sendMessage(String topic, Object event) {
        try {
            String messagePayload = objectMapper.writeValueAsString(event);

            // 현재 스레드의 MDC에서 Trace ID 꺼내기
            String traceId = MDC.get("traceId");

            // MessageBuilder를 사용하여 페이로드(JSON)와 카프카 헤더(Trace ID)를 함께 포장
            Message<String> kafkaMessage = MessageBuilder
                    .withPayload(messagePayload)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader("X-Trace-Id", traceId != null ? traceId : "UNKNOWN-TRACE")
                    .build();

            // 4. 포장된 메시지 전송
            kafkaTemplate.send(kafkaMessage);
            log.info("[카프카 발송 성공] 토픽: {}, TraceID: {}, 메시지: {}", topic, traceId, messagePayload);

        } catch (JsonProcessingException e) {
            log.error("카프카 메시지 직렬화 에러. 토픽: {}", topic, e);
        }
    }
}
