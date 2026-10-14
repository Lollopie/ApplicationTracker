package com.jobtracker.main;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserDetailModel implements UserDetails {

    private final UUID id;
    private final String username;
    private final String password;
    private final List<GrantedAuthority> authorities;

    public UserDetailModel(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.authorities = new ArrayList<>();
    }
    public UserDetailModel(UUID id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = new ArrayList<>();
    }

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return this.password;
    }

    @Override
    @NullMarked
    public String getUsername() {
        return this.username;
    }

    @NullMarked
    public UUID getId() {
        return this.id;
    }
}
