package com.sxp.StockXpress.dto;

import com.sxp.StockXpress.enums.ChangeType;
import lombok.*;

import java.time.LocalDateTime;
// InventoryLogResponse.java (Audit Trail)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLogResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long changedBy;
    private String changedByName;
    private Integer oldQuantity;
    private Integer newQuantity;
    private ChangeType changeType;
    private String reason;
    private LocalDateTime timestamp;
}
