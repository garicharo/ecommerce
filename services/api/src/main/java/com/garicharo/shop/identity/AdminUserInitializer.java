package com.garicharo.shop.identity;

import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationArguments;

@Component
public class AdminUserInitializer implements ApplicationRunner {
    
    private final UserRepository userRepository;
    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;

    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, AdminProperties adminProperties ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (userRepository.findByEmail(adminProperties.getEmail()).isPresent()) {
            return;
        }
        User user = new User();
        user.setEmail(adminProperties.getEmail());
        user.setDisplayName("Admin");
        user.setPasswordHash(passwordEncoder.encode(adminProperties.getPassword()));
        user.setRole(Role.ADMIN);
        user.setCreatedAt(OffsetDateTime.now());
        userRepository.save(user);
    }
    
}
