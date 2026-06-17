package org.example.corepayorderservice.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer orderPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderState state;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderLineItem> orderLineItems = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column
    private LocalDateTime updatedAt;

    @Builder
    public Order(Long userId){
        this.userId = userId;
        this.orderPrice = 0;
        this.state = OrderState.PENDING_STOCK;
    }

    public void addLineItem(OrderLineItem item) {
        this.orderLineItems.add(item);
        item.setOrder(this);
    }

    public Boolean isSameUser(Long userId){
        return Objects.equals(this.userId, userId);
    }

    // 총액 업데이트 메서드
    public void updateOrderPrice(Integer totalAmount) {
        this.orderPrice = totalAmount;
    }

    // 재고 선점 성공 (Product 서버 Kafka 응답)
    public void reserveStock() {
        validateState(OrderState.PENDING_STOCK);
        this.state = OrderState.STOCK_RESERVED;
    }

    // 재고 선점 실패
    public void failStock() {
        validateState(OrderState.PENDING_STOCK);
        this.state = OrderState.STOCK_FAILED;
    }

    // 결제 요청
    public void requestPayment() {
        validateState(OrderState.STOCK_RESERVED);
        this.state = OrderState.PAYMENT_REQUESTED;
    }

    // 결제 성공
    public void completePayment() {
        validateState(OrderState.PAYMENT_REQUESTED);
        this.state = OrderState.COMPLETED;
    }

    // 결제 실패
    public void failPayment() {
        validateState(OrderState.PAYMENT_REQUESTED);
        this.state = OrderState.CANCELLED;
    }

    // 취소 (사용자 직접 취소 등)
    public void cancel() {
        this.state = OrderState.CANCELLED;
    }

    // 환불
    public void refund() {
        validateState(OrderState.COMPLETED);
        this.state = OrderState.REFUNDED;
    }

    // 만료 (스케줄러)
    public void expire() {
        validateState(OrderState.STOCK_RESERVED);
        this.state = OrderState.EXPIRED;
    }

    //환불 실패(DLT Consumer)
    public void refundFailed() {
        // 어떤 상태에서도 강제 마킹 (운영 개입 필요 상황)
        this.state = OrderState.REFUND_FAILED;
    }

    private void validateState(OrderState required) {
        if (this.state != required) {
            throw new IllegalStateException(
                    String.format("잘못된 상태 전이: 현재=%s, 필요=%s", this.state, required)
            );
        }
    }
}