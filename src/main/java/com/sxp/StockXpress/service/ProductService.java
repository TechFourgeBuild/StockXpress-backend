package com.sxp.StockXpress.service;

import com.sxp.StockXpress.dto.ProductRequest;
import com.sxp.StockXpress.dto.ProductResponse;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final UserService userService;
    private final InventoryLogService inventoryLogService;

    //  ReentrantLock with Fairness Policy for bulk stock updates
    private final ReentrantLock bulkStockLock = new ReentrantLock(true);


    //  CREATE PRODUCT (ADMIN only)
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .availableQuantity(request.getAvailableQuantity() != null ? request.getAvailableQuantity() : 0)
                .category(request.getCategory().toLowerCase())
                .imageUrl(request.getImageUrl())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("✅ Product created: {} (ID: {})", savedProduct.getName(), savedProduct.getId());
        return mapToResponse(savedProduct);
    }


    //  GET PRODUCT BY ID
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return mapToResponse(product);
    }


    //  GET ALL PRODUCTS
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    //  GET PRODUCTS BY CATEGORY
    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findByCategory(category)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    //  UPDATE PRODUCT (ADMIN only)
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());

        Product updatedProduct = productRepository.save(product);
        log.info("✅ Product updated: {} (ID: {})", updatedProduct.getName(), updatedProduct.getId());
        return mapToResponse(updatedProduct);
    }


    //  DELETE PRODUCT (ADMIN only)
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        productRepository.delete(product);
        log.info("🗑️ Product deleted: {} (ID: {})", product.getName(), product.getId());
    }


    //  UPDATE STOCK — WITH OPTIMISTIC LOCKING & RETRY
    @Transactional
    @Retryable(
            value = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public ProductResponse updateStock(Long productId, Integer newQuantity, String reason) {
        //  Fetch current user from SecurityContext
        User currentUser = getCurrentUser();

        //  Get product with version
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        Integer oldQuantity = product.getAvailableQuantity();

        //  Update stock using version-based query
        int updatedRows = productRepository.updateStockWithVersion(
                productId,
                newQuantity,
                product.getVersion()
        );

        if (updatedRows == 0) {
            log.warn("⚠️ Optimistic locking failure for product ID: {}", productId);
            throw new OptimisticLockingFailureException(
                    "Stock update failed due to concurrent modification. Please retry."
            );
        }

        //  Refresh product to get updated version
        Product updatedProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));


        //  Log inventory change
        logInventoryChange(updatedProduct, currentUser, oldQuantity, newQuantity, reason);

        log.info("✅ Stock updated: {} (ID: {}) | {} → {} | Version: {}",
                updatedProduct.getName(), productId, oldQuantity, newQuantity, updatedProduct.getVersion());

        return mapToResponse(updatedProduct);
    }


    //  BULK STOCK UPDATE — WITH REENTRANTLOCK
    @Transactional
    public void bulkStockUpdate(List<StockUpdateRequest> updates) {
        User currentUser = getCurrentUser();

        bulkStockLock.lock();
        try {
            log.info("🔒 Bulk stock update started by: {}", currentUser.getEmail());

            for (StockUpdateRequest request : updates) {
                try {
                    updateStock(request.getProductId(), request.getQuantity(), request.getReason());
                } catch (OptimisticLockingFailureException e) {
                    log.error("❌ Failed to update product ID: {} - {}", request.getProductId(), e.getMessage());

                }
            }

            log.info("🔓 Bulk stock update completed");

        } finally {
            bulkStockLock.unlock();
        }
    }


    //  GET LOW STOCK PRODUCTS
    public List<ProductResponse> getLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    //  GET PRODUCT ENTITY (For internal use)
    public Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }


    // PRIVATE HELPERS
    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .availableQuantity(product.getAvailableQuantity())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private void logInventoryChange(Product product, User user, Integer oldQuantity,
                                    Integer newQuantity, String reason) {
        InventoryLog logEntry = InventoryLog.builder()
                .product(product)
                .changedBy(user)
                .oldQuantity(oldQuantity)
                .newQuantity(newQuantity)
                .changeType(oldQuantity < newQuantity ? ChangeType.ADDED : ChangeType.DEDUCTED)
                .reason(reason)
                .build();

        inventoryLogRepository.save(logEntry);
        log.info("📝 Inventory log created for product: {}", product.getName());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByEmail(email);
    }

    // ✅ Inner class for bulk update request
    public static class StockUpdateRequest {
        private Long productId;
        private Integer quantity;
        private String reason;

        // Getters and setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}