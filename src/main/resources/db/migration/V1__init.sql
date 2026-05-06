-- 주문 (Orders) 테이블
CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        order_price INT NOT NULL,    -- 총 결제 금액
                        state VARCHAR(50) NOT NULL,  -- 상태 (READY, COMPLETED, CANCELED 등)
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        -- 사용자 기반 조회가 빈번하므로 인덱스를 걸어줍니다.
                        INDEX idx_orders_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_line_items (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  order_id BIGINT NOT NULL,    -- 논리적 FK (실제 외래 키 제약조건은 없음)
                                  product_id BIGINT NOT NULL,  -- 논리적 FK (Product 도메인)
                                  price INT NOT NULL,          -- 해당 상품의 단가 (할인 적용 후)
                                  amount INT NOT NULL,         -- 구매 수량

                                  -- 인덱스 추가: order_id 기반 조회가 빈번하므로 인덱스를 걸어줍니다.
                                  INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
