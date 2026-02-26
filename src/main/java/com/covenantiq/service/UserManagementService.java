package com.covenantiq.service;

import com.covenantiq.domain.UserAccount;
import com.covenantiq.dto.request.CreateUserRequest;
import com.covenantiq.dto.request.UpdateUserRolesRequest;
import com.covenantiq.dto.response.UserResponse;
import com.covenantiq.enums.ActivityEventType;
import com.covenantiq.exception.ConflictException;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.exception.UnprocessableEntityException;
import com.covenantiq.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UserManagementService {

    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    public UserManagementService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            ActivityLogService activityLogService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists");
        }
        if (userAccountRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }
        validatePasswordPolicy(request.password());
        List<String> roles = normalizeRoles(request.roles());
        if (roles.isEmpty()) {
            throw new UnprocessableEntityException("At least one role is required");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRolesCsv(String.join(",", roles));
        user.setActive(true);
        user.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        UserAccount saved = userAccountRepository.save(user);
        activityLogService.logEvent(ActivityEventType.USER_CREATED, "User", saved.getId(), null, "User created");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Pageable pageable) {
        return userAccountRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        return toResponse(userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found")));
    }

    @Transactional
    public UserResponse updateUserRoles(Long userId, UpdateUserRolesRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found"));
        List<String> roles = normalizeRoles(request.roles());
        if (roles.isEmpty()) {
            throw new UnprocessableEntityException("At least one role is required");
        }
        user.setRolesCsv(String.join(",", roles));
        UserAccount saved = userAccountRepository.save(user);
        activityLogService.logEvent(ActivityEventType.USER_UPDATED, "User", saved.getId(), null, "User roles updated");
        return toResponse(saved);
    }

    @Transactional
    public void deactivateUser(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found"));

        if (containsRole(user.getRolesCsv(), "ADMIN")) {
            long activeAdmins = userAccountRepository.countByActiveTrueAndRolesCsvContaining("ADMIN");
            if (user.isActive() && activeAdmins <= 1) {
                throw new UnprocessableEntityException("Cannot deactivate last ADMIN user");
            }
        }
        user.setActive(false);
        userAccountRepository.save(user);
        activityLogService.logEvent(ActivityEventType.USER_DEACTIVATED, "User", userId, null, "User deactivated");
    }

    private void validatePasswordPolicy(String password) {
        if (!PASSWORD_POLICY.matcher(password).matches()) {
            throw new UnprocessableEntityException(
                    "Password must be at least 8 chars and include uppercase, lowercase, digit, special character"
            );
        }
    }

    private List<String> normalizeRoles(List<String> roles) {
        return roles.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private boolean containsRole(String rolesCsv, String role) {
        return Arrays.stream(rolesCsv.split(","))
                .map(String::trim)
                .anyMatch(role::equals);
    }

    private UserResponse toResponse(UserAccount user) {
        List<String> roles = Arrays.stream(user.getRolesCsv().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isActive(),
                roles,
                user.getCreatedAt()
        );
    }
}
