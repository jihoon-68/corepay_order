package org.example.corepayorderservice.order;

import org.example.corepayorderservice.order.application.OrderService;
import org.example.corepayorderservice.order.application.command.CreatedOrderCommand;
import org.example.corepayorderservice.order.domain.Order;
import org.example.corepayorderservice.order.domain.OrderState;
import org.example.corepayorderservice.order.infrastructure.db.OrderRepository;
import org.example.corepayorderservice.product.application.BasicProductSnapshotService;
import org.example.corepayorderservice.product.presentation.ProductSnapshotDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, ports = {9092})
public class OrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private BasicProductSnapshotService productSnapshotService;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private CountDownLatch latch;
    private String receivedMessage;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        latch = new CountDownLatch(1);
        receivedMessage = null;

        // 카프카 컨슈머 준비 대기 (결제 서버에서 배운 필수 로직)
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
    }

    // 오더 서버가 발행한 이벤트를 검증하기 위한 가짜 컨슈머
    @KafkaListener(topics = "order-created-topic", groupId = "test-order-group")
    public void listen(String message) {
        this.receivedMessage = message;
        this.latch.countDown();
    }

    @Test
    @DisplayName("주문 생성 시 상품 정보를 조회하고, DB 저장 후 카프카 이벤트를 발행한다.")
    void createOrder_Success() throws Exception {
        // Given: 상품 정보 모킹
        ProductSnapshotDto product = new ProductSnapshotDto(1L, "테스트 상품", 10000, 10); // 10% 할인
        given(productSnapshotService.getProductInfo(anyLong())).willReturn(product);

        CreatedOrderCommand command = new CreatedOrderCommand(1L, 1L, 2); // 유저1, 상품1, 수량2

        // When: 주문 생성
        orderService.creat(command);

        // Then 1: 카프카 이벤트 발행 검증 (비동기 대기)
        boolean messageReceived = latch.await(5, TimeUnit.SECONDS);

        assertThat(messageReceived).isTrue();
        assertThat(receivedMessage).contains("\"totalPrice\":18000"); // (10000 - 1000) * 2

        // Then 2: DB 저장 상태 검증
        Order savedOrder = orderRepository.findAll().get(0);
        assertThat(savedOrder.getOrderPrice()).isEqualTo(18000);
        assertThat(savedOrder.getState()).isEqualTo(OrderState.READY);
    }
}