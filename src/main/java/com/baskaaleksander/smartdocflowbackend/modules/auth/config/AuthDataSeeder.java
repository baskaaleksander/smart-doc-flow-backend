package com.baskaaleksander.smartdocflowbackend.modules.auth.config;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Role;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.AuthUserCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.AuthUserQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RoleCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RoleQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthDataSeeder implements ApplicationRunner {

    private final RoleCommandPort roleCommandPort;
    private final RoleQueryPort roleQueryPort;
    private final AuthUserCommandPort authUserCommandPort;
    private final AuthUserQueryPort authUserQueryPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (authUserQueryPort.count() > 0) return;

        Role adminRole = roleQueryPort.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRole("ROLE_ADMIN");
                    r.setDescription("Admin role");
                    return roleCommandPort.save(r);
                });
        Role userRole = roleQueryPort.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRole("ROLE_USER");
                    r.setDescription("User role");
                    return roleCommandPort.save(r);
                });
        Role reviewerRole = roleQueryPort.findByName("ROLE_REVIEW")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRole("ROLE_REVIEW");
                    r.setDescription("Reviewer role");
                    return roleCommandPort.save(r);
                });

        AuthUser admin = new AuthUser();
        admin.setUsername("admin");
        admin.setEmail("admin@smartdocflow.loacl");
        admin.setPassword(passwordEncoder.encode("Admin#12345"));
        admin.setRoles(Set.of(adminRole.getRole()));
        admin.setActive(true);

        AuthUser reviewer = new AuthUser();
        reviewer.setUsername("reviewer");
        reviewer.setEmail("reviewer@smartdocflow.local");
        reviewer.setPassword(passwordEncoder.encode("Reviewer#12345"));
        reviewer.setRoles(Set.of(reviewerRole.getRole()));
        reviewer.setActive(true);

        AuthUser user = new AuthUser();
        user.setUsername("user");
        user.setEmail("user@smartdocflow.local");
        user.setPassword(passwordEncoder.encode("User#12345"));
        user.setRoles(Set.of(userRole.getRole()));
        user.setActive(true);

        authUserCommandPort.save(admin);
        authUserCommandPort.save(reviewer);
        authUserCommandPort.save(user);

        System.out.println("Seeded 3 users");
    }
}
