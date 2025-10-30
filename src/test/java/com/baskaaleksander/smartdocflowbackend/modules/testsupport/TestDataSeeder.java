package com.baskaaleksander.smartdocflowbackend.modules.testsupport;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataRoleRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class TestDataSeeder {

    @Bean
    public TestSeeder testSeeder() {
        return new TestSeeder();
    }

    public static class TestSeeder {

        @Autowired
        private SpringDataUserRepository userRepository;

        @Autowired
        private SpringDataRoleRepository roleRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Transactional
        public void seedAccountsIfNotExists() {
            RoleEntity adminRole = roleRepository.findByRole("ROLE_ADMIN")
                    .orElseGet(() -> {
                        RoleEntity e = new RoleEntity();
                        e.setRole("ROLE_ADMIN");
                        e.setDescription("Admin role");
                        return roleRepository.save(e);
                    });
            RoleEntity userRole = roleRepository.findByRole("ROLE_USER")
                    .orElseGet(() -> {
                        RoleEntity e = new RoleEntity();
                        e.setRole("ROLE_USER");
                        e.setDescription("User role");
                        return roleRepository.save(e);
                    });
            RoleEntity reviewerRole = roleRepository.findByRole("ROLE_REVIEW")
                    .orElseGet(() -> {
                        RoleEntity e = new RoleEntity();
                        e.setRole("ROLE_REVIEW");
                        e.setDescription("Reviewer role");
                        return roleRepository.save(e);
                    });


            String adminEmail = "admin@smartdocflow.local";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                UserEntity admin = new UserEntity();
                admin.setEmail(adminEmail);
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("Admin#12345"));
                admin.setRoles(Set.of(adminRole, userRole));
                userRepository.save(admin);
            }
            String reviewerEmail = "reviewer@smartdocflow.local";
            if (userRepository.findByEmail(reviewerEmail).isEmpty()) {
                UserEntity reviewer = new UserEntity();
                reviewer.setEmail(reviewerEmail);
                reviewer.setUsername("reviewer");
                reviewer.setPassword(passwordEncoder.encode("Reviewer#12345"));
                reviewer.setRoles(Set.of(reviewerRole, userRole));
                userRepository.save(reviewer);
            }
            String userEmail = "user@smartdocflow.local";
            if (userRepository.findByEmail(userEmail).isEmpty()) {
                UserEntity user = new UserEntity();
                user.setEmail(userEmail);
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("User#12345"));
                user.setRoles(Set.of(userRole));
                userRepository.save(user);
            }
        }
    }
}
