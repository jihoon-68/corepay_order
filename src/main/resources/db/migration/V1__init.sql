-- 3. 주문 (Orders) 테이블
CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,    -- FK 제약조건 없이 논리적 관계만 유지
                        product_id BIGINT NOT NULL, -- FK 제약조건 없이 논리적 관계만 유지
                        order_price INT NOT NULL,   -- 결제 시점의 가격 스냅샷 (매우 중요)
                        amount INT NOT NULL,
                        state ENUM('READY', 'COMPLETED', 'CANCELED', 'REFUNDED') NOT NULL DEFAULT 'READY',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 특정 유저의 주문 목록 조회, 특정 상품의 주문 내역 조회를 위한 인덱스 추가
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_product_id ON orders(product_id);
