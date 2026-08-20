package com.sxp.StockXpress.config;

//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StockXpress API Documentation")
                        .description("""
                                StockXpress is a B2B Inventory & Warehouse Management System.
                                
                                ## Features:
                                - JWT Authentication & Authorization
                                - Role-Based Access Control (CUSTOMER, WAREHOUSE_MANAGER, ADMIN)
                                - Product Management (CRUD, Stock Updates)
                                - Order Management (Place, Cancel, Status, Payment)
                                - Inventory Logs (Audit Trail)
                                - Admin Dashboard & User Management
                                - Concurrency Handling (Optimistic Locking, ReentrantLock)
                                """)
                        .version("v1.0")
                        .contact(new Contact()
                                .name("StockXpress Team")
                                .email("support@stockxpress.com")
                                .url("https://github.com/yourusername/stockxpress"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token in the format: **Bearer &lt;token&gt;**")));
    }

}
