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

    // ✅ Find orders by user ID
    List<Order> findByUserId(Long userId);

    // ✅ Find orders by user ID with sorting (latest first)
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // ✅ Find orders by status
    List<Order> findByStatus(OrderStatus status);

    // ✅ Find orders by user and status
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    // ✅ Check if order exists by order number
    boolean existsByOrderNumber(String orderNumber);

    // ✅ Find order by order number
    Optional<Order> findByOrderNumber(String orderNumber);

    // ✅ Get total revenue (for admin dashboard)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM Order o WHERE o.status != 'CANCELLED'")
    Double getTotalRevenue();  // ✅ Returns 0.0 if no orders

    // ✅ Get orders in last N days
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :date")
    List<Order> findOrdersFromLastDays(@Param("date") LocalDateTime date);

    void deleteByUserId(Long userId);

}
