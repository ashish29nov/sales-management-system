
package com.sales.management.config;

import com.sales.management.model.User;
import com.sales.management.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner createUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // =========================
            // ADMIN USER
            // =========================

            if (userRepository.findByEmail("admin@gseindia.com")
                    .isEmpty()) {

                User admin = new User();

                admin.setUsername("admin");
                admin.setEmployeeId("ADMIN001");
                admin.setName("System Admin");
                admin.setEmail("admin@gseindia.com");

                admin.setPassword(
                        passwordEncoder.encode("Admin@1996")
                );

                admin.setRole("ADMIN");
                admin.setMobile("9999906021");
                admin.setCity("Delhi");
                admin.setActive(true);

                userRepository.save(admin);
            }


            // =========================
            // SALESPERSON USER
            // =========================

            if (userRepository.findByEmail("ashish@boot.com")
                    .isEmpty()) {

                User salesperson = new User();

                salesperson.setUsername("ashish");
                salesperson.setEmployeeId("EMP1001");
                salesperson.setName("Ashish");
                salesperson.setEmail("ashish@boot.com");

                salesperson.setPassword(
                        passwordEncoder.encode("Ashish@1996")
                );

                salesperson.setRole("SALESPERSON");
                salesperson.setMobile("9695987071");
                salesperson.setCity("Noida");
                salesperson.setActive(true);

                userRepository.save(salesperson);
            }
        };
    }
}
