package com.baskaaleksander.smartdocflowbackend.modules.auth.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsernameWithRoles(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username " + username + " not found"));

        List<SimpleGrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getRole()))
                .toList();

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                user.isActive()
        );
    }
}
