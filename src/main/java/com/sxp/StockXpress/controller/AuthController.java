package com.sxp.StockXpress.controller;

import com.sxp.StockXpress.dto.AuthResponse;
import com.sxp.StockXpress.dto.LoginRequest;
import com.sxp.StockXpress.dto.RegisterRequest;
import com.sxp.StockXpress.dto.UserProfileResponse;
import com.sxp.StockXpress.entity.User;
import com.sxp.StockXpress.enums.Role;
import com.sxp.StockXpress.exception.DuplicateResourceException;
import com.sxp.StockXpress.exception.TokenRefreshException;
import com.sxp.StockXpress.repository.UserRepository;
import com.sxp.StockXpress.security.jwt.JwtUtil;
import com.sxp.StockXpress.service.RefreshTokenService;
import com.sxp.StockXpress.service.UserService;
import com.sxp.StockXpress.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "APIs for authentication and authorization")
public class AuthController {
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private CookieUtil cookieUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserService userService;


    //  REGISTER — create new user
    @PostMapping("/register")
    @Operation(summary = "Register", description = "Registers a new user with CUSTOMER role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Email already exists")
    })
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        //  Check if email already exists
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already registered!");
        }

        //  Create new user
        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))  //  BCrypt encode
                .role(Role.CUSTOMER)  //  Default role
                .build();

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully! Please login.");
    }


    //  LOGIN — issuing both access + refresh token
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates user and returns JWT access token with refresh token cookie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),loginRequest.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(()->new RuntimeException("User not found"));

        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.createNewRefreshToken(user);

        cookieUtil.addRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(new AuthResponse(
                accessToken,
                user.getRole().name(),
                user.getEmail(),
                user.getName()
        ));
    }


    //  REFRESH — grab new accessToken, along with rotation
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest request,HttpServletResponse response) {
        String oldRefreshToken = cookieUtil.getRefreshTokenFromCookie(request);
        if (oldRefreshToken == null) {
            // throw new TokenRefreshException("Refresh token missing — please login again");
            throw new TokenRefreshException("Session expired. Please sign in again.");
        }

        RefreshTokenService.RotationResult result =
                refreshTokenService.rotateRefreshToken(oldRefreshToken);

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User.builder()
                        .username(result.user.getEmail())
                        .password(result.user.getPassword())
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + result.user.getRole().name()))
                        .build();

        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        cookieUtil.addRefreshTokenCookie(response, result.newRefreshToken);

        return ResponseEntity.ok(new AuthResponse(newAccessToken));
    }


    //  LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request , HttpServletResponse response) {
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);
        if (refreshToken != null) {
            refreshTokenService.revokeToken(refreshToken);
        }
        cookieUtil.clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves profile of the authenticated user")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getCurrentUserProfile());
    }
}
