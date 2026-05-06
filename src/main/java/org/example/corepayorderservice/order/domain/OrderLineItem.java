package org.example.corepayorderservice.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_line_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 💡 핵심: 외래 키 제약조건을 물리적으로 생성하지 않음 (ConstraintMode.NO_CONSTRAINT)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Order order;

    @Column(nullable = false)
    private Long productId; // Product 서비스의 ID (논리적 연결)

    @Column(nullable = false)
    private Integer price; // 할인 적용된 최종 단가

    @Column(nullable = false)
    private Integer amount; // 수량

    @Builder
    public OrderLineItem(Long productId, Integer price, Integer amount) {
        this.productId = productId;
        this.price = price;
        this.amount = amount;
    }

    // 연관관계 편의 메서드를 위한 Setter (Order 엔티티에서만 호출)
    protected void setOrder(Order order) {
        this.order = order;
    }
}