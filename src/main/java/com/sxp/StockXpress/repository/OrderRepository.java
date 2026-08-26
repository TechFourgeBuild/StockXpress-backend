package com.sxp.StockXpress.repository;

import com.sxp.StockXpress.entity.Order;
import com.sxp.StockXpress.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {


    List<Order> findByUserId(Long userId);


    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);


    List<Order> findByStatus(OrderStatus status);


    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);


    boolean existsByOrderNumber(String orderNumber);


    Optional<Order> findByOrderNumber(String orderNumber);


    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM Order o WHERE o.status != 'CANCELLED'")
    Double getTotalRevenue();


    @Query("SELECT o FROM Order o WHERE o.createdAt >= :date")
    List<Order> findOrdersFromLastDays(@Param("date") LocalDateTime date);

    // Entity-based delete (not bulk) — ensures OrderItem cascade-delete triggers properly
    void deleteByUserId(Long userId);

}
