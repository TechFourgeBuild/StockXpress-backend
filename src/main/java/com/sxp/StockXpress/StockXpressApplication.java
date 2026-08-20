package com.sxp.StockXpress;

import com.sxp.StockXpress.entity.User;
import com.sxp.StockXpress.enums.Role;
import com.sxp.StockXpress.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableRetry
@SpringBootApplication
public class StockXpressApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockXpressApplication.class, args);
	}

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.findByEmail("admin@stockxpress.com").isPresent()) {
                User admin = User.builder()
                        .name("SuperAdmin")
                        .email("admin@stockxpress.com")
                        .password(passwordEncoder.encode("Admin@123"))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                System.out.println("✅ Default Admin created!");
                System.out.println("📧 Email: admin@stockxpress.com");
                System.out.println("🔑 Password: Admin@123");
            }
        };
    }

}
