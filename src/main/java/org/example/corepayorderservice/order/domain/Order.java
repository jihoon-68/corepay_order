package org.example.corepayorderservice.order.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
        this.state = OrderState.READY;
    }

    public void addLineItem(OrderLineItem item) {
        this.orderLineItems.add(item);
        item.setOrder(this);
    }

    // 총액 업데이트 메서드
    public void updateOrderPrice(Integer totalAmount) {
        this.orderPrice = totalAmount;
    }


    public void cancel() {
        this.state = OrderState.CANCELED;
    }

    public void complete() {
        this.state = OrderState.COMPLETED;
    }

    public void refund() {
        this.state = OrderState.REFUNDED;
    }
}