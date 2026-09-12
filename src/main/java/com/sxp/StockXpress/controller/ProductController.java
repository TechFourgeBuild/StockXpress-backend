package com.sxp.StockXpress.controller;

import com.sxp.StockXpress.dto.ProductRequest;
import com.sxp.StockXpress.dto.ProductResponse;
import com.sxp.StockXpress.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "APIs for managing products")
public class ProductController {

    private final ProductService productService;


    //  PUBLIC — Get all products
    @GetMapping
    @Operation(
            summary = "Get all products",
            description = "Retrieves a list of all products. Accessible to all authenticated users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("📦 Fetching all products");
        return ResponseEntity.ok(productService.getAllProducts());
    }


    //  PUBLIC — Get product by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        log.info("📦 Fetching product with ID: {}", id);
        return ResponseEntity.ok(productService.getProductById(id));
    }


    //  PUBLIC — Get products by category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable String category) {
        log.info("📦 Fetching products by category: {}", category);
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }


    //  PUBLIC — Get low stock products
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts(
            @RequestParam(defaultValue = "5") int threshold) {
        log.info("📦 Fetching low stock products (threshold: {})", threshold);
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }


    //  ADMIN — Create product
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new product",
            description = "Creates a new product. Only accessible to ADMIN role.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    })
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        log.info("📦 Creating product: {}", request.getName());
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    //  ADMIN — Update product
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        log.info("📦 Updating product with ID: {}", id);
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }


    //  ADMIN — Delete product
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("🗑️ Deleting product with ID: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }


    //  WAREHOUSE + ADMIN — Update stock (with Optimistic Locking)
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(
            summary = "Update product stock",
            description = "Updates stock quantity for a product. Accessible to WAREHOUSE_MANAGER and ADMIN roles.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic locking conflict - retry")
    })
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            @RequestParam String reason) {
        log.info("📦 Updating stock for product ID: {} | New: {} | Reason: {}", id, quantity, reason);
        return ResponseEntity.ok(productService.updateStock(id, quantity, reason));
    }


    //  WAREHOUSE + ADMIN — Bulk stock update (with ReentrantLock)
    @PatchMapping("/stock/bulk")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    public ResponseEntity<Void> bulkStockUpdate(@RequestBody List<ProductService.StockUpdateRequest> updates) {
        log.info("📦 Bulk stock update for {} products", updates.size());
        productService.bulkStockUpdate(updates);
        return ResponseEntity.ok().build();
    }
}