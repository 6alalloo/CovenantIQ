package com.covenantiq.service;

import com.covenantiq.domain.UserAccount;
import com.covenantiq.dto.response.AuthResponse;
import com.covenantiq.exception.AuthenticationFailedException;
import com.covenantiq.repository.UserAccountRepository;
import com.covenantiq.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginReturnsTokensAndResetsFailedAttempts() {
        UserAccount user = user("analyst@demo.com", "hashed", "ANALYST, ADMIN");
        user.setFailedLoginAttempts(3);
        user.setLockoutUntilUtc(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));

        when(userAccountRepository.findByUsername("analyst@demo.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Demo123!", "hashed")).thenReturn(true);
        when(jwtService.createAccessToken(user)).thenReturn("access-token");
        when(jwtService.createRefreshToken(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login("analyst@demo.com", "Demo123!");

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("analyst@demo.com", response.username());
        assertEquals(2, response.roles().size());
        assertEquals("ANALYST", response.roles().get(0));
        assertEquals("ADMIN", response.roles().get(1));
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockoutUntilUtc());
        verify(userAccountRepository).save(user);
    }

    @Test
    void loginFailsForInactiveAccount() {
        UserAccount user = user("analyst@demo.com", "hashed", "ANALYST");
        user.setActive(false);
        when(userAccountRepository.findByUsername("analyst@demo.com")).thenReturn(Optional.of(user));

        AuthenticationFailedException ex = assertThrows(
                AuthenticationFailedException.class,
                () -> authService.login("analyst@demo.com", "Demo123!")
        );

        assertEquals("Account is inactive", ex.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginFailsWhenAccountIsLocked() {
        UserAccount user = user("analyst@demo.com", "hashed", "ANALYST");
        user.setLockoutUntilUtc(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));
        when(userAccountRepository.findByUsername("analyst@demo.com")).thenReturn(Optional.of(user));

        AuthenticationFailedException ex = assertThrows(
                AuthenticationFailedException.class,
                () -> authService.login("analyst@demo.com", "Demo123!")
        );

        assertEquals("Account is locked. Try again later", ex.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginFailedPasswordIncrementsAttempts() {
        UserAccount user = user("analyst@demo.com", "hashed", "ANALYST");
        user.setFailedLoginAttempts(2);
        when(userAccountRepository.findByUsername("analyst@demo.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        AuthenticationFailedException ex = assertThrows(
                AuthenticationFailedException.class,
                () -> authService.login("analyst@demo.com", "bad")
        );

        assertEquals("Invalid username or password", ex.getMessage());
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getFailedLoginAttempts());
        assertNull(captor.getValue().getLockoutUntilUtc());
    }

    @Test
    void loginFailedPasswordLocksAccountAfterFiveAttempts() {
        UserAccount user = user("analyst@demo.com", "hashed", "ANALYST");
        user.setFailedLoginAttempts(4);
        when(userAccountRepository.findByUsername("analyst@demo.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class, () -> authService.login("analyst@demo.com", "bad"));

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getFailedLoginAttempts());
        assertNotNull(captor.getValue().getLockoutUntilUtc());
    }

    @Test
    void refreshReturnsNewAccessTokenWhenRefreshTokenIsValid() {
        UserAccount user = user("analyst@demo.com", "hashed", "ANALYST");
        Claims claims = org.mockito.Mockito.mock(Claims.class);

        when(jwtService.parse("refresh-token")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        when(claims.getSubject()).thenReturn("analyst@demo.com");
        when(userAccountRepository.findByUsername("analyst@demo.com")).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(user)).thenReturn("new-access-token");

        AuthResponse response = authService.refresh("refresh-token");

        assertEquals("new-access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("analyst@demo.com", response.username());
        assertEquals(1, response.roles().size());
        assertEquals("ANALYST", response.roles().get(0));
    }

    @Test
    void refreshFailsWhenTokenCannotBeParsed() {
        when(jwtService.parse("bad-token")).thenThrow(new RuntimeException("bad token"));

        AuthenticationFailedException ex = assertThrows(
                AuthenticationFailedException.class,
                () -> authService.refresh("bad-token")
        );

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void refreshFailsWhenTokenTypeIsNotRefresh() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtService.parse("access-token")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(false);

        AuthenticationFailedException ex = assertThrows(
                AuthenticationFailedException.class,
                () -> authService.refresh("access-token")
        );

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void refreshFailsForInactiveUser() {
        UserAccount user = user("analyst@demo.com", "hashed", "ANALYST");
        user.setActive(false);
        Claims claims = org.mockito.Mockito.mock(Claims.class);

        when(jwtService.parse("refresh-token")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        when(claims.getSubject()).thenReturn("analyst@demo.com");
        when(userAccountRepository.findByUsername("analyst@demo.com")).thenReturn(Optional.of(user));

        AuthenticationFailedException ex = assertThrows(
                AuthenticationFailedException.class,
                () -> authService.refresh("refresh-token")
        );

        assertEquals("Account is inactive", ex.getMessage());
    }

    private UserAccount user(String username, String passwordHash, String rolesCsv) {
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setRolesCsv(rolesCsv);
        user.setActive(true);
        return user;
    }
}
