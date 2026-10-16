package com.prescription_Management_REST_API.security;

import com.prescription_Management_REST_API.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Loads an {@link com.prescription_Management_REST_API.entity.AppUser} from the
 * database and adapts it into a Spring Security {@link UserDetails}.
 *
 * Spring Security calls this on every authenticated request (Basic Auth header).
 * The {@code @Transactional(readOnly=true)} keeps the DB interaction lightweight.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public AppUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(appUser -> new User(
                        appUser.getUsername(),
                        appUser.getPassword(),
                        appUser.isEnabled(),
                        true,  // accountNonExpired
                        true,  // credentialsNonExpired
                        true,  // accountNonLocked
                        // Authority is the full enum name: "ROLE_ADMIN" or "ROLE_USER"
                        List.of(new SimpleGrantedAuthority(appUser.getRole().name()))
                ))
                .orElseThrow(() ->
                        new UsernameNotFoundException("No account found for username: " + username));
    }
}

