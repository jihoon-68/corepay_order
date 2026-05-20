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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class BasicProductSnapshotService implements ProductSnapshotService{

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Map<Long, ProductSnapshotDto> getProductInfos(List<Long> productIds) {
        Map<Long, ProductSnapshotDto> result = new HashMap<>();
        List<Long> cacheMiss = new ArrayList<>();

        // 1. Redis 일괄 조회
        for (Long id : productIds) {
            String key = "product:snapshot:" + id;
            String redisData = redisTemplate.opsForValue().get(key);
            if (redisData != null) {
                try {
                    result.put(id, objectMapper.readValue(redisData, ProductSnapshotDto.class));
                } catch (JsonProcessingException e) {
                    cacheMiss.add(id);
                }
            } else {
                cacheMiss.add(id);
            }
        }

        // 2. Redis 미스만 DB에서 IN 쿼리로 1번에 조회
        if (!cacheMiss.isEmpty()) {
            snapshotRepository.findAllByProductIdIn(cacheMiss)
                    .forEach(s -> result.put(s.getProductId(), ProductSnapshotDto.from(s)));
        }

        return result;
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
