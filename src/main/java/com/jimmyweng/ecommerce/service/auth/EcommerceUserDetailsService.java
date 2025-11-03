package com.jimmyweng.ecommerce.service.auth;

import com.jimmyweng.ecommerce.constant.Role;
import com.jimmyweng.ecommerce.model.User;
import com.jimmyweng.ecommerce.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class EcommerceUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public EcommerceUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPasswordHash(), mapAuthorities(user.getRole()));
    }

    private Collection<? extends GrantedAuthority> mapAuthorities(Role role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
