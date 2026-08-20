package com.sxp.StockXpress.entity;

import com.sxp.StockXpress.enums.ChangeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id",nullable = false)
    private Product product;     // ✅ Kis product ka stock change hua

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by",nullable = false)
    private User changedBy;        // ✅ Kisne change kiya (User)

    @Column(nullable = false)
    private Integer oldQuantity;   // ✅ Previous stock value

    @Column(nullable = false)
    private Integer newQuantity;    // ✅ New stock value

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType changeType;    // ✅ ADDED, DEDUCTED, ADJUSTED

    @Column(length = 500)
    private String reason;     // ✅ Kyun hua change (Order placed, Manual update, etc.)

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;   // ✅ Kab hua change
}
