package com.aicareerresume.controller;

import com.aicareerresume.dto.*;
import com.aicareerresume.entity.Role;
import com.aicareerresume.entity.User;
import com.aicareerresume.exception.BadRequestException;
import com.aicareerresume.repository.UserRepository;
import com.aicareerresume.security.JwtTokenProvider;
import com.aicareerresume.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BadRequestException("Email is already registered.");
        }

        User user = new User(
                req.email(),
                passwordEncoder.encode(req.password()),
                req.fullName(),
                Role.USER
        );
        userRepository.save(user);

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = tokenProvider.generateToken(auth);

        AuthResponse resp = new AuthResponse(jwt, "Bearer", user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", resp));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = tokenProvider.generateToken(auth);

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.getId()).orElseThrow();

        AuthResponse resp = new AuthResponse(jwt, "Bearer", user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", resp));
    }
}
