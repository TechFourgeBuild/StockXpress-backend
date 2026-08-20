package com.sxp.StockXpress.enums;

public enum ChangeType {
    ADDED,       // ✅ Stock increase (WAREHOUSE_MANAGER restocked)
    DEDUCTED,    // ❌ Stock decrease (Customer placed order)
    ADJUSTED     // 🔄 Manual adjustment (correcting inventory errors)
}