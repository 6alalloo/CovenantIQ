package com.covenantiq.controller;

import com.covenantiq.dto.request.CreateUserRequest;
import com.covenantiq.dto.request.UpdateUserRolesRequest;
import com.covenantiq.dto.response.UserResponse;
import com.covenantiq.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userManagementService.createUser(request);
    }

    @GetMapping
    public Page<UserResponse> getUsers(Pageable pageable) {
        return userManagementService.getUsers(pageable);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userManagementService.getUserById(id);
    }

    @PatchMapping("/{id}/roles")
    public UserResponse updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest request) {
        return userManagementService.updateUserRoles(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        userManagementService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
}
