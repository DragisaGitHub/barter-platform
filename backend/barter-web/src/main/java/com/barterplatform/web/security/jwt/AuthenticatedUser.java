package com.barterplatform.web.security.jwt;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUser implements UserDetails {

    @Getter
    private final UUID userUuid;
    private final String username;
    @Getter
    private final List<String> roles;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(UUID userUuid, String username, List<String> roles) {
        this.userUuid = userUuid;
        this.username = username;
        this.roles = roles;
        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }
}

