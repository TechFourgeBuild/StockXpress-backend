package com.sxp.StockXpress.repository;

import com.sxp.StockXpress.entity.InventoryLog;
import com.sxp.StockXpress.enums.ChangeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryLogRepository extends JpaRepository<InventoryLog,Long> {

    List<InventoryLog> findByProductId(Long productId);


    List<InventoryLog> findByChangeType(ChangeType changeType);


    List<InventoryLog> findByProductIdOrderByTimestampDesc(Long productId);


    List<InventoryLog> findByChangedById(Long userId);


    @Query("SELECT i FROM InventoryLog i WHERE i.product.id = :productId ORDER BY i.timestamp DESC LIMIT 1")
    Optional<InventoryLog> findLatestLogByProductId(@Param("productId") Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM InventoryLog i WHERE i.changedBy.id = :userId")
    void deleteByChangedById(@Param("userId") Long userId);
}
