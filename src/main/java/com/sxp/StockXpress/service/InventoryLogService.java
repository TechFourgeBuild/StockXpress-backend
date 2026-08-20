package com.sxp.StockXpress.service;

import com.sxp.StockXpress.dto.InventoryLogResponse;
import com.sxp.StockXpress.entity.InventoryLog;
import com.sxp.StockXpress.entity.Product;
import com.sxp.StockXpress.entity.User;
import com.sxp.StockXpress.enums.ChangeType;
import com.sxp.StockXpress.exception.ResourceNotFoundException;
import com.sxp.StockXpress.repository.InventoryLogRepository;
import com.sxp.StockXpress.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryLogService {

    private final InventoryLogRepository inventoryLogRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    // ============================================================
    // 1️⃣ LOG A STOCK CHANGE (Internal Use)
    // ============================================================
    public void logStockChange(Long productId, Long userId, Integer oldQuantity,
                               Integer newQuantity, ChangeType changeType, String reason) {

        // ✅ Directly fetch product from repository
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
        User user = userService.getUserById(userId);

        InventoryLog logEntry = InventoryLog.builder()
                .product(product)
                .changedBy(user)
                .oldQuantity(oldQuantity)
                .newQuantity(newQuantity)
                .changeType(changeType)
                .reason(reason)
                .build();

        inventoryLogRepository.save(logEntry);
        log.info("📝 Inventory log created: Product: {}, {} → {}, Type: {}, Reason: {}",
                product.getName(), oldQuantity, newQuantity, changeType, reason);
    }

    // ============================================================
    // 2️⃣ GET ALL LOGS FOR A PRODUCT
    // ============================================================
    public List<InventoryLogResponse> getLogsByProduct(Long productId) {
        // ✅ Verify product exists
        return inventoryLogRepository.findByProductId(productId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // 3️⃣ GET ALL LOGS FOR A USER
    // ============================================================
    public List<InventoryLogResponse> getLogsByUser(Long userId) {
        // ✅ Verify user exists
        userService.getUserById(userId);   // why we are using it here cause we are not even holding it somewhere for usecase

        return inventoryLogRepository.findByChangedById(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // 4️⃣ GET LOGS BY CHANGE TYPE
    // ============================================================
    public List<InventoryLogResponse> getLogsByChangeType(ChangeType changeType) {
        return inventoryLogRepository.findByChangeType(changeType)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // 5️⃣ GET LATEST LOG FOR A PRODUCT
    // ============================================================
    public InventoryLogResponse getLatestLogByProduct(Long productId) {
        // ✅ Verify product exists
//        productService.getProductEntity(productId);

        return inventoryLogRepository.findLatestLogByProductId(productId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory log found for product ID: " + productId));
    }

    // ============================================================
    // 6️⃣ GET ALL LOGS (ADMIN only)
    // ============================================================
    public List<InventoryLogResponse> getAllLogs() {
        return inventoryLogRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================
    private InventoryLogResponse mapToResponse(InventoryLog log) { /* didn't understood that how it is helping to another methods of
         this class*/
        return InventoryLogResponse.builder()
                .id(log.getId())
                .productId(log.getProduct().getId())
                .productName(log.getProduct().getName())
                .changedBy(log.getChangedBy().getId())
                .changedByName(log.getChangedBy().getName())
                .oldQuantity(log.getOldQuantity())
                .newQuantity(log.getNewQuantity())
                .changeType(log.getChangeType())
                .reason(log.getReason())
                .timestamp(log.getTimestamp())
                .build();
    }
}
