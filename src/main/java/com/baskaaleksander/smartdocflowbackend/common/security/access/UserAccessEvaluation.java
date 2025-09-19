package com.baskaaleksander.smartdocflowbackend.common.security.access;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("userAccess")
public class UserAccessEvaluation {

    private final UserRepository userRepository;

    public UserAccessEvaluation(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean canManage(String userId, Authentication authentication) {

        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        if (roles.contains("ROLE_ADMIN")) return true;

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(() -> new ResourceNotFoundException("User not found"));


        return UUID.fromString(userId).equals(user.getId());
    }
}
