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

            if (userRepository.findByEmail("admin@company.com")
                    .isEmpty()) {

                User admin = new User();

                admin.setEmployeeId("ADMIN001");
                admin.setName("System Admin");
                admin.setEmail("admin@company.com");

                admin.setPassword(
                    passwordEncoder.encode("Admin@123")
                );

                admin.setRole("ADMIN");
                admin.setMobile("9999999999");
                admin.setCity("Delhi");
                admin.setActive(true);

                userRepository.save(admin);
            }


            if (userRepository.findByEmail("rahul@company.com")
                    .isEmpty()) {

                User salesperson = new User();

                salesperson.setEmployeeId("EMP1001");
                salesperson.setName("Rahul");
                salesperson.setEmail("rahul@company.com");

                salesperson.setPassword(
                    passwordEncoder.encode("Sales@123")
                );

                salesperson.setRole("SALESPERSON");
                salesperson.setMobile("9999999998");
                salesperson.setCity("Delhi");
                salesperson.setActive(true);

                userRepository.save(salesperson);
            }
        };
    }
}