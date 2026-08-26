package com.sxp.StockXpress.repository;

import com.sxp.StockXpress.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {


    List<OrderItem> findByOrderId(Long orderId);


    List<OrderItem> findByProductId(Long productId);


    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.product.id = :productId")
    Integer getTotalQuantitySoldByProduct(@Param("productId") Long productId);


    @Query("SELECT SUM(oi.totalPrice) FROM OrderItem oi WHERE oi.product.id = :productId")
    Double getTotalRevenueByProduct(@Param("productId") Long productId);

    void deleteByOrderId(Long orderId);
}
