package org.example.corepayorderservice.product.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "product_snapshot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSnapshot {

    @Id
    private Long productId;

    private String name;
    private int price;
    private int discount;

    @Builder
    public ProductSnapshot(Long productId, String name, int price, int discount) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.discount = discount;
    }

    public void updateInfo(String name, int price, int discount) {
        this.name = name;
        this.price = price;
        this.discount = discount;
    }
}
