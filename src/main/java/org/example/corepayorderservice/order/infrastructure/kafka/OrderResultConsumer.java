package org.example.corepayorderservice.order.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.application.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderResultConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    // [보상 트랜잭션] 실패(취소) 이벤트 수신
    @KafkaListener(topics = "order-cancel-topic", groupId = "order-group")
    public void consumeOrderCancel(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            Long orderId = jsonNode.get("orderId").asLong();
            String reason = jsonNode.has("reason") ? jsonNode.get("reason").asText() : "UNKNOWN_ERROR";

            orderService.cancelOrder(orderId, reason);
        } catch (JsonProcessingException e) {
            log.error("보상 트랜잭션 메시지 파싱 에러", e);
        }
    }

    // [결제 완료] 최종 성공 이벤트 수신
    @KafkaListener(topics = "order-completed-topic", groupId = "order-group")
    public void consumeOrderComplete(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            Long orderId = jsonNode.get("orderId").asLong();

            orderService.completeOrder(orderId);
        } catch (JsonProcessingException e) {
            log.error("성공 메시지 파싱 에러", e);
        }
    }

    // [환불 완료] 환불 이벤트 수신
    @KafkaListener(topics = "order-refunded-topic", groupId = "order-group")
    public void consumeOrderRefund(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            Long orderId = jsonNode.get("orderId").asLong();

            orderService.refundOrder(orderId);
        } catch (JsonProcessingException e) {
            log.error("환불 메시지 파싱 에러", e);
        }
    }
}