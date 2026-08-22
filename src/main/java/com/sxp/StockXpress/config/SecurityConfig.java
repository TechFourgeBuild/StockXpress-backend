package com.sxp.StockXpress.config;

import com.sxp.StockXpress.security.jwt.JwtAuthEntryPoint;
import com.sxp.StockXpress.security.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired private UserDetailsService userDetailsService;
    @Autowired @Lazy private PasswordEncoder passwordEncoder;
    @Autowired private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired private JwtAuthEntryPoint jwtAuthEntryPoint;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new
                DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // ✅ 12 rounds of hashing
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ============================================================
    // ✅ CORS CONFIGURATION — Frontend access ke liye
    // ============================================================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 🎯 Frontend origins allow karo:
        // (a) Local development — Vite default port 5173
        // (b) Production — Vercel domain
        // Yeh combination frontend ko backend APIs access karne degi
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",  // ✅ Local Vite dev server
                "http://localhost:5174",  // ✅ Alternative local port
                "https://stock-xpress-frontend.vercel.app",  // ✅ Production Vercel domain
                "https://stockxpress-git-main.vercel.app",  // ✅ Main branch preview
                "https://stockxpress-*.vercel.app"  // ✅ All preview deployments
        ));

        // 🎯 Cookies (Refresh Token) allow karo
        configuration.setAllowCredentials(true);

        // ✅ Allow all headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",    // ✅ JWT token
                "Content-Type",     // ✅ JSON body
                "Accept",           // ✅ Response type
                "X-Requested-With", // ✅ AJAX requests
                "Cookie",           // ✅ Refresh token cookie
                "Set-Cookie"        // ✅ Set-Cookie header
        ));

        // ✅ Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ✅ Expose headers to frontend
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Set-Cookie"
        ));

        // ✅ Max age for preflight requests (1 hour)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // ✅ ENABLE CORS
                .csrf(AbstractHttpConfigurer::disable)
                // 🎯 Access-token stateless hai. Refresh-token cookie hai, PAR:
                // (a) HttpOnly + path-scoped hai, (b) SameSite=Strict hoga production
                // ye combination CSRF-risk ko significantly kam kar deta hai
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/register",  // ✅ Register public
                                "/api/v1/auth/login",   // ✅ Login public
                                "/api/v1/auth/refresh-token",  // ✅ Refresh public (cookie se aayega)
                                "/api/v1/public/**",


                                // ✅ SWAGGER PATHS — API DOCS
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/api-docs/**",
                                "/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs"
                        ).permitAll()
                        .requestMatchers("/api/v1/auth/logout").authenticated()  // 🔒 LOGOUT - authenticated user
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")  // 👑 ADMIN ONLY
                        .requestMatchers("/api/v1/warehouseManager/**").hasAnyRole("WAREHOUSE_MANAGER", "ADMIN") // 🏭 WAREHOUSE_MANAGER + ADMIN (Admin = God!)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthEntryPoint)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}