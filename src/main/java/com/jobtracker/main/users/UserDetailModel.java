package com.jobtracker.main.users;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

public class UserDetailModel implements UserDetails, CredentialsContainer {

    private final UUID id;
    private final String username;
    private char[] password;
    private final List<GrantedAuthority> authorities;

    public UserDetailModel(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash().toCharArray();
        this.authorities = new ArrayList<>();
    }
    public UserDetailModel(UUID id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password.toCharArray();
        this.authorities = new ArrayList<>();
    }

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return this.password != null ? new String(this.password) : null;
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

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
