package org.example.corepayorderservice.product.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Entity
@Getter
@Table(name = "product_snapshot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSnapshot implements Persistable<Long> {

    @Id
    private Long id;

    @Column(scale = 255,nullable = false)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int discount;

    @Transient
    private boolean isNew = true;

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Builder
    public ProductSnapshot(Long id, String name, int price, int discount) {
        this.id = id;
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
