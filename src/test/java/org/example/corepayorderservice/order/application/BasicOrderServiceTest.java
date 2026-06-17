package org.example.corepayorderservice.order.application;

import feign.FeignException;
import org.example.corepayorderservice.order.application.command.*;
import org.example.corepayorderservice.order.domain.Order;
import org.example.corepayorderservice.order.domain.OrderState;
import org.example.corepayorderservice.order.infrastructure.db.OrderRepository;
import org.example.corepayorderservice.order.infrastructure.feign.PaymentFeignClient;
import org.example.corepayorderservice.order.infrastructure.feign.ProductStockClient;
import org.example.corepayorderservice.order.infrastructure.feign.dto.PaymentResponse;
import org.example.corepayorderservice.order.infrastructure.feign.dto.StockReserveResponse;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderCancelledEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.StockConfirmEvent;
import org.example.corepayorderservice.order.presentation.dto.OrderCreateResponse;
import org.example.corepayorderservice.order.presentation.dto.OrderDto;
import org.example.corepayorderservice.order.presentation.dto.PaymentResultDto;
import org.example.corepayorderservice.product.application.ProductSnapshotService;
import org.example.corepayorderservice.product.presentation.ProductSnapshotDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicOrderServiceTest {

    @InjectMocks
    private BasicOrderService orderService;

    @Mock private OrderRepository orderRepository;
    @Mock private ProductSnapshotService productSnapshotService;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private ProductStockClient productStockClient;
    @Mock private PaymentFeignClient paymentFeignClient;

    // ── 픽스처 ──────────────────────────────────────────

    private Order makeOrder(Long id, OrderState state) {
        Order order = Order.builder().userId(1L).build();
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "orderPrice", 10000);
        ReflectionTestUtils.setField(order, "state", state);
        return order;
    }

    private CreatedOrderCommand makeCreateCommand() {
        return CreatedOrderCommand.builder()
                .userId(1L)
                .items(List.of(
                        new CreatedOrderCommand.OrderItemCommand(1L, 2),
                        new CreatedOrderCommand.OrderItemCommand(2L, 1)
                ))
                .build();
    }

    private ProductSnapshotDto makeSnapshot(Long id, int price, int discount) {
        return new ProductSnapshotDto(id, "상품" + id, price, discount);
    }

    // ── creat() ─────────────────────────────────────────

    @Nested
    @DisplayName("주문 생성")
    class CreateOrder {

        @Test
        @DisplayName("재고 선점 성공 → STOCK_RESERVED 상태, reserved 응답 반환")
        void create_stockReserveSuccess() {
            // given
            CreatedOrderCommand command = makeCreateCommand();
            given(productSnapshotService.getProductInfos(any()))
                    .willReturn(Map.of(
                            1L, makeSnapshot(1L, 10000, 10),
                            2L, makeSnapshot(2L, 5000, 0)
                    ));
            given(productStockClient.reserveStock(any()))
                    .willReturn(new StockReserveResponse(true, List.of()));

            // when
            OrderCreateResponse response = orderService.create(command);

            // then
            assertThat(response.status()).isEqualTo("STOCK_RESERVED");
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("재고 선점 실패 → STOCK_FAILED 상태, failed 응답 반환")
        void create_stockReserveFailed() {
            // given
            CreatedOrderCommand command = makeCreateCommand();
            given(productSnapshotService.getProductInfos(any()))
                    .willReturn(Map.of(
                            1L, makeSnapshot(1L, 10000, 0),
                            2L, makeSnapshot(2L, 5000, 0)
                    ));
            given(productStockClient.reserveStock(any()))
                    .willReturn(new StockReserveResponse(false, List.of(1L)));

            // when
            OrderCreateResponse response = orderService.create(command);

            // then
            assertThat(response.status()).isEqualTo("STOCK_FAILED");
            assertThat(response.outOfStockItems()).contains(1L);
        }

        @Test
        @DisplayName("재고 서버 통신 오류 → RuntimeException 발생")
        void create_stockReserveFeignError() {
            // given
            CreatedOrderCommand command = makeCreateCommand();
            given(productSnapshotService.getProductInfos(any()))
                    .willReturn(Map.of(
                            1L, makeSnapshot(1L, 10000, 0),
                            2L, makeSnapshot(2L, 5000, 0)
                    ));
            given(productStockClient.reserveStock(any()))
                    .willThrow(mock(FeignException.class));

            // when & then
            assertThatThrownBy(() -> orderService.create(command))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("재고 서버와 통신 중 오류");
        }

        @Test
        @DisplayName("할인율 적용 총액 계산 검증")
        void create_discountPriceCalculation() {
            // given: price=10000, discount=10 → 9000 * 2 = 18000
            CreatedOrderCommand command = CreatedOrderCommand.builder()
                    .userId(1L)
                    .items(List.of(new CreatedOrderCommand.OrderItemCommand(1L, 2)))
                    .build();
            given(productSnapshotService.getProductInfos(any()))
                    .willReturn(Map.of(1L, makeSnapshot(1L, 10000, 10)));
            given(productStockClient.reserveStock(any()))
                    .willReturn(new StockReserveResponse(true, List.of()));

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);

            // when
            orderService.create(command);

            // then
            verify(orderRepository).save(captor.capture());
            assertThat(captor.getValue().getOrderPrice()).isEqualTo(18000);
        }
    }

    // ── requestPayment() ────────────────────────────────

    @Nested
    @DisplayName("결제 요청")
    class RequestPayment {

        @Test
        @DisplayName("결제 성공 → COMPLETED 상태, StockConfirmEvent 발행, success 응답")
        void payment_success() {
            // given
            Order order = makeOrder(1L, OrderState.STOCK_RESERVED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(paymentFeignClient.pay(any()))
                    .willReturn(new PaymentResponse(true, null));

            // when
            PaymentResultDto result = orderService.requestPayment(1L);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.orderId()).isEqualTo(1L);
            assertThat(order.getState()).isEqualTo(OrderState.COMPLETED);

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(publisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(StockConfirmEvent.class);
        }

        @Test
        @DisplayName("결제 실패 → CANCELLED 상태, OrderCancelledEvent 발행, fail 응답")
        void payment_fail() {
            // given
            Order order = makeOrder(1L, OrderState.STOCK_RESERVED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(paymentFeignClient.pay(any()))
                    .willReturn(new PaymentResponse(false, "잔액 부족"));

            // when
            PaymentResultDto result = orderService.requestPayment(1L);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.failReason()).isEqualTo("잔액 부족");
            assertThat(order.getState()).isEqualTo(OrderState.CANCELLED);

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(publisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(OrderCancelledEvent.class);
        }

        @Test
        @DisplayName("결제 타임아웃 → PAYMENT_REQUESTED 상태 유지, RuntimeException 발생")
        void payment_timeout() {
            // given
            Order order = makeOrder(1L, OrderState.STOCK_RESERVED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            given(paymentFeignClient.pay(any()))
                    .willThrow(mock(FeignException.GatewayTimeout.class));

            // when & then
            assertThatThrownBy(() -> orderService.requestPayment(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("결제 결과를 확인할 수 없습니다");

            assertThat(order.getState()).isEqualTo(OrderState.PAYMENT_REQUESTED);
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("STOCK_RESERVED 아닌 상태에서 결제 요청 → IllegalStateException")
        void payment_wrongState() {
            // given
            Order order = makeOrder(1L, OrderState.PENDING_STOCK);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.requestPayment(1L))
                    .isInstanceOf(IllegalStateException.class);

            verify(paymentFeignClient, never()).pay(any());
        }
    }

    // ── cancelOrder() ────────────────────────────────────

    @Nested
    @DisplayName("주문 취소")
    class CancelOrder {

        @Test
        @DisplayName("주문 취소 → CANCELLED 상태, PaymentCancelEvent 발행")
        void cancel_success() {
            // given
            Order order = makeOrder(1L, OrderState.STOCK_RESERVED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            CancelOrderCommand command = CancelOrderCommand.builder()
                    .id(1L).reason(CancelReason.CUSTOMER_CANCEL).build();

            // when
            orderService.cancelOrder(command);

            // then
            assertThat(order.getState()).isEqualTo(OrderState.CANCELLED);

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(publisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(PaymentCancelEvent.class);

            PaymentCancelEvent event = (PaymentCancelEvent) captor.getValue();
            assertThat(event.orderId()).isEqualTo(1L);
            assertThat(event.reason()).isEqualTo(CancelReason.CUSTOMER_CANCEL);
        }
    }

    // ── refundOrder() ────────────────────────────────────

    @Nested
    @DisplayName("환불")
    class RefundOrder {

        @Test
        @DisplayName("환불 → REFUNDED 상태")
        void refund_success() {
            // given
            Order order = makeOrder(1L, OrderState.COMPLETED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.refundOrder(RefundOrderCommand.builder().id(1L).build());

            // then
            assertThat(order.getState()).isEqualTo(OrderState.REFUNDED);
        }
    }

    // ── get() / getList() ────────────────────────────────

    @Nested
    @DisplayName("주문 조회")
    class GetOrder {

        @Test
        @DisplayName("존재하는 주문 조회 성공")
        void get_success() {
            // given
            Order order = makeOrder(1L, OrderState.COMPLETED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatCode(() -> orderService.get(1L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 → RuntimeException")
        void get_notFound() {
            given(orderRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.get(99L))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("전체 주문 목록 페이징 조회")
        void getList_success() {
            // given
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));

            List<Order> orders = List.of(
                    makeOrder(1L, OrderState.COMPLETED),
                    makeOrder(2L, OrderState.CANCELLED)
            );
            Page<Order> orderPage = new PageImpl<>(orders, pageable, orders.size());

            given(orderRepository.findAll(pageable)).willReturn(orderPage);

            // when
            Page<OrderDto> result = orderService.getList(pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent().get(0).state()).isEqualTo(OrderState.COMPLETED.toString());
            assertThat(result.getContent().get(1).state()).isEqualTo(OrderState.CANCELLED.toString());
        }
    }
}