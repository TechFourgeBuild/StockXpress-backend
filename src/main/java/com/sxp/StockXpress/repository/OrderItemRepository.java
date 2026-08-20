package com.sxp.StockXpress.repository;

import com.sxp.StockXpress.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {

    // ✅ Find all items in a specific order
    List<OrderItem> findByOrderId(Long orderId);

    // ✅ Find all items for a specific product (sales history)
    List<OrderItem> findByProductId(Long productId);

    // ✅ Get total quantity sold for a product
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.product.id = :productId")
    Integer getTotalQuantitySoldByProduct(@Param("productId") Long productId);

    // ✅ Get total revenue for a product
    @Query("SELECT SUM(oi.totalPrice) FROM OrderItem oi WHERE oi.product.id = :productId")
    Double getTotalRevenueByProduct(@Param("productId") Long productId);

    void deleteByOrderId(Long orderId);
}
