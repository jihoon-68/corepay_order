package org.example.corepayorderservice.product.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.product.domain.ProductSnapshot;
import org.example.corepayorderservice.product.infrastructure.db.ProductSnapshotRepository;
import org.example.corepayorderservice.product.infrastructure.kafka.event.ProductCreatedEvent;
import org.example.corepayorderservice.product.presentation.ProductSnapshotDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class BasicProductSnapshotService implements ProductSnapshotService{

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ProductSnapshotDto getProductInfo(Long productId) {
        String key = "product:snapshot:" + productId;

        // 1. Redis 찔러보기 (한정판/타임세일 등 Hot Data)
        String redisData = redisTemplate.opsForValue().get(key);
        if (redisData != null) {
            try {
                log.info("상품 ID: {}", productId);
                return objectMapper.readValue(redisData, ProductSnapshotDto.class);
            } catch (JsonProcessingException e) {
                log.error("Redis 데이터 파싱 에러", e);
                // 파싱 실패 시 조용히 아래 DB 조회 로직으로 넘어가게(Fallback) 둡니다.
            }
        }

        // 2. Redis에 없으면? (평상시) 주문 서버의 내장 DB에서 조회
        log.info("상품 ID: {}", productId);
        return snapshotRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("상품 정보가 존재하지 않거나 판매 중지되었습니다."));
    }

    @Override
    @Transactional
    public void productSnapshotSave(ProductCreatedEvent event) {
        ProductSnapshot snapshot = snapshotRepository.findById(event.productId())
                .orElseGet(() -> ProductSnapshot.builder()
                        .productId(event.productId())
                        .name(event.name())
                        .price(event.price())
                        .discount(event.discount())
                        .build());

        // 이미 존재하는 상품의 가격 변동일 수도 있으니 값 덮어쓰기 (업데이트)
        snapshot.updateInfo(event.name(), event.price(), event.discount());

        snapshotRepository.save(snapshot);
        log.info(" [상품 요약본 동기화 완료] 상품 ID: {}, 이름: {}", event.productId(), event.name());
    }
}
