package com.covenantiq.service;

import com.covenantiq.domain.UserAccount;
import com.covenantiq.dto.response.AuthResponse;
import com.covenantiq.exception.AuthenticationFailedException;
import com.covenantiq.repository.UserAccountRepository;
import com.covenantiq.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse login(String username, String password) {
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid username or password"));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("Account is inactive");
        }
        if (user.getLockoutUntilUtc() != null && user.getLockoutUntilUtc().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new AuthenticationFailedException("Account is locked. Try again later");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            onFailedLogin(user);
            throw new AuthenticationFailedException("Invalid username or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockoutUntilUtc(null);
        userAccountRepository.save(user);

        return new AuthResponse(
                jwtService.createAccessToken(user),
                jwtService.createRefreshToken(user),
                user.getUsername(),
                parseRoles(user)
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (RuntimeException ex) {
            throw new AuthenticationFailedException("Invalid refresh token");
        }

        if (!jwtService.isRefreshToken(claims)) {
            throw new AuthenticationFailedException("Invalid refresh token");
        }

        String username = claims.getSubject();
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));
        if (!user.isActive()) {
            throw new AuthenticationFailedException("Account is inactive");
        }

        return new AuthResponse(
                jwtService.createAccessToken(user),
                refreshToken,
                user.getUsername(),
                parseRoles(user)
        );
    }

    private void onFailedLogin(UserAccount user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= 5) {
            user.setLockoutUntilUtc(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15));
            user.setFailedLoginAttempts(0);
        }
        userAccountRepository.save(user);
    }

    private List<String> parseRoles(UserAccount user) {
        return Arrays.stream(user.getRolesCsv().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
