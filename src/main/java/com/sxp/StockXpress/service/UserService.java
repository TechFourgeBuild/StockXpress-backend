package com.sxp.StockXpress.service;

import com.sxp.StockXpress.dto.UserProfileResponse;
import com.sxp.StockXpress.entity.User;
import com.sxp.StockXpress.exception.ResourceNotFoundException;
import com.sxp.StockXpress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;


    //  GET USER BY ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }


    //  GET USER BY EMAIL
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }


    //  GET CURRENT LOGGED-IN USER
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return getUserByEmail(email);
    }


    //  GET USER PROFILE (For API response)
    public UserProfileResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }


    //  CHECK IF USER EXISTS
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
