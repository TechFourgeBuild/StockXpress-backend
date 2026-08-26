package com.sxp.StockXpress.repository;

import com.sxp.StockXpress.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {


    List<Product> findByCategory(String category);


    @Query("SELECT p FROM Product p WHERE p.availableQuantity < :threshold")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);


    @Modifying
    @Query("UPDATE Product p SET p.availableQuantity = :newQuantity, p.version = p.version + 1 WHERE p.id = :id AND p.version = :version")
    int updateStockWithVersion(@Param("id") Long id,
                               @Param("newQuantity") Integer newQuantity,
                               @Param("version") Integer version);
}
