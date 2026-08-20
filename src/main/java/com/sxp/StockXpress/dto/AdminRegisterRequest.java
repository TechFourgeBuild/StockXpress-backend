package com.sxp.StockXpress.dto;

import com.sxp.StockXpress.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AdminRegisterRequest {
    private String name;
    private String email;
    private String password;
    private Role role;  // ✅ WAREHOUSE_MANAGER or ADMIN
}