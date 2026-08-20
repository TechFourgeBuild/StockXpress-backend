package com.sxp.StockXpress.controller;

import com.sxp.StockXpress.dto.InventoryLogResponse;
import com.sxp.StockXpress.enums.ChangeType;
import com.sxp.StockXpress.service.InventoryLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "inventory-controller", description = "APIs for Audits and logs checking")
public class InventoryController {

    private final InventoryLogService inventoryLogService;

    // ============================================================
    // 🏭 WAREHOUSE + ADMIN — Get logs by product
    // ============================================================
    @GetMapping("/logs/product/{productId}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    public ResponseEntity<List<InventoryLogResponse>> getLogsByProduct(@PathVariable Long productId) {
        log.info("📄 Fetching inventory logs for product ID: {}",productId);
        return ResponseEntity.ok(inventoryLogService.getLogsByProduct(productId));
    }

    // ============================================================
    // 🏭 WAREHOUSE + ADMIN — Get logs by user
    // ============================================================
    @GetMapping("/logs/user/{userId}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    public ResponseEntity<List<InventoryLogResponse>> getLogsByUser(@PathVariable Long userId) {
        log.info("📄 Fetching inventory logs by user ID: {}", userId);
        return ResponseEntity.ok(inventoryLogService.getLogsByUser(userId));
    }

    // ============================================================
    // 🏭 WAREHOUSE + ADMIN — Get logs by change type
    // ============================================================
    @GetMapping("logs/type/{changeType}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    public ResponseEntity<List<InventoryLogResponse>> getLogsByChangeType(@PathVariable ChangeType changeType) {
        log.info("📝 Fetching inventory logs by change type: {}", changeType);
        return ResponseEntity.ok(inventoryLogService.getLogsByChangeType(changeType));
    }

    // ============================================================
    // 🏭 WAREHOUSE + ADMIN — Get latest log for product
    // ============================================================
    @GetMapping("/logs/latest/{productId}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    public ResponseEntity<InventoryLogResponse> getLatestLogsByProduct(@PathVariable Long productId) {
        log.info("📝 Fetching latest inventory log for product ID: {}", productId);
        return ResponseEntity.ok(inventoryLogService.getLatestLogByProduct(productId));
    }

    // ============================================================
    // 👑 ADMIN — Get all logs
    // ============================================================
    @GetMapping("/logs/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryLogResponse>> getAllLogs() {
        log.info("📝 Fetching all inventory logs (Admin only)");
        return ResponseEntity.ok(inventoryLogService.getAllLogs());
    }
}
