package com.sxp.StockXpress.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private String role;
    private String email;
    private String name;

    // ✅ Sirf accessToken wala constructor
    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
    }

    // ✅ Constructor with all fields
    public AuthResponse(String accessToken, String role, String email, String name) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.role = role;
        this.email = email;
        this.name = name;
    }
}
