package com.flowpilot.auth;

import com.flowpilot.user.AppUser;
import com.flowpilot.user.UserRepository;
import com.flowpilot.user.UserRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@EnableConfigurationProperties(InitialAdminProperties.class)
public class InitialAdminUserInitializer {

    @Bean
    ApplicationRunner initialAdminUserRunner(InitialAdminUserService service) {
        return args -> service.createInitialAdminIfConfigured();
    }

    @Bean
    InitialAdminUserService initialAdminUserService(
            InitialAdminProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return new InitialAdminUserService(properties, userRepository, passwordEncoder);
    }

    static class InitialAdminUserService {

        private static final Logger LOGGER = LoggerFactory.getLogger(InitialAdminUserService.class);

        private final InitialAdminProperties properties;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        InitialAdminUserService(
                InitialAdminProperties properties,
                UserRepository userRepository,
                PasswordEncoder passwordEncoder
        ) {
            this.properties = properties;
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
        }

        @Transactional
        void createInitialAdminIfConfigured() {
            properties.validate();
            if (!properties.isConfigured()) {
                return;
            }

            String email = properties.normalizedEmail();
            if (userRepository.existsByEmail(email)) {
                LOGGER.info("Initial admin user already exists: {}", email);
                return;
            }

            AppUser admin = new AppUser(
                    email,
                    properties.resolvedDisplayName(),
                    passwordEncoder.encode(properties.password()),
                    UserRole.ADMIN
            );
            userRepository.save(admin);
            LOGGER.info("Initial admin user created: {}", email);
        }
    }
}
