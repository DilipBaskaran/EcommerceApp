package com.ideas2it.ecommerceapp.repository;

import com.ideas2it.ecommerceapp.model.Order;
import com.ideas2it.ecommerceapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);

    Page<Order> findByUser(User user, Pageable pageable);

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.orderDate < :cutoffDate")
    List<Order> findStaleOrders(Order.OrderStatus status, LocalDateTime cutoffDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :startDate")
    Long countOrdersSince(LocalDateTime startDate);

    // For analytics
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.orderDate BETWEEN :startDate AND :endDate")
    Long countOrdersByStatusAndDateRange(Order.OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);
}
