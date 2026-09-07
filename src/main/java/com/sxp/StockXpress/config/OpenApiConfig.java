package com.sxp.StockXpress.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {

        //  PRODUCTION SERVER — DEPLOYED URL
        Server productionServer = new Server();
        productionServer.setUrl("https://stockxpress.duckdns.org");
        productionServer.setDescription("Production Server (DuckDNS)");

        //  LOCAL SERVER
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Development Server");

        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(productionServer, localServer))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerAuthScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("StockXpress API Documentation")
                .description("""
                        StockXpress is a B2B Inventory & Warehouse Management System.

                        ## Features
                        - JWT Authentication & Authorization
                        - Role-Based Access Control (CUSTOMER, WAREHOUSE_MANAGER, ADMIN)
                        - Product Management (CRUD, Stock Updates)
                        - Order Management (Place, Cancel, Status, Payment)
                        - Inventory Logs (Audit Trail)
                        - Admin Dashboard & User Management
                        - Concurrency Handling (Optimistic Locking, ReentrantLock)

                        ## Links
                        - Frontend (Live): https://stock-xpress-frontend.vercel.app
                        - Backend (Live): https://stockxpress.duckdns.org
                        - Backend Repository: https://github.com/TechFourgeBuild/StockXpress-backend
                        - Frontend Repository: https://github.com/TechFourgeBuild/StockXpress-frontend
                        """)
                .version("v1.0")
                .contact(new Contact()
                        .name("StockXpress Team")
                        .email("support@stockxpress.com")
                        .url("https://stock-xpress-frontend.vercel.app"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT token in the format: **Bearer &lt;token&gt;**");
    }
}