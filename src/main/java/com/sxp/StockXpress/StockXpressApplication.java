package com.sxp.StockXpress;

import com.sxp.StockXpress.entity.User;
import com.sxp.StockXpress.enums.Role;
import com.sxp.StockXpress.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableRetry
@SpringBootApplication
public class StockXpressApplication {

    @Value("${admin.email}")
    String email;

    @Value("${admin.password}")
    String password;

	public static void main(String[] args) {
		SpringApplication.run(StockXpressApplication.class, args);
	}

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.findByEmail(email).isPresent()) {
                User admin = User.builder()
                        .name("SuperAdmin")
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                System.out.println("✅ Default Admin created!");
                System.out.println("📧 Email: "+email);
            }
        };
    }

}
