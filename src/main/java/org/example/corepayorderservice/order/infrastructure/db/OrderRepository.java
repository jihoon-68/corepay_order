package org.example.corepayorderservice.order.infrastructure.db;

import org.example.corepayorderservice.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
