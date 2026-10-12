package com.jobtracker.main;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public void register(RegisterDto registerDto) {
        this.userRepository.save(new User(registerDto.username(), passwordEncoder.encode(registerDto.password())));
    }
}
