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
    // ✅ Find all logs for a product
    List<InventoryLog> findByProductId(Long productId);

    // ✅ Find logs by change type (ADDED, DEDUCTED, ADJUSTED)
    List<InventoryLog> findByChangeType(ChangeType changeType);

    // ✅ Find logs for a product ordered by timestamp (latest first)
    List<InventoryLog> findByProductIdOrderByTimestampDesc(Long productId);

    // ✅ Find logs by user who made the change
    List<InventoryLog> findByChangedById(Long userId);

    // ✅ Get latest log for a product
    @Query("SELECT i FROM InventoryLog i WHERE i.product.id = :productId ORDER BY i.timestamp DESC LIMIT 1")
    Optional<InventoryLog> findLatestLogByProductId(@Param("productId") Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM InventoryLog i WHERE i.changedBy.id = :userId")
    void deleteByChangedById(@Param("userId") Long userId);
}
