package tr.com.hacettepe.tams.auth_service.service;

import tr.com.hacettepe.tams.auth_service.dto.CreateUserRequest;
import tr.com.hacettepe.tams.auth_service.dto.UpdateUserRequest;
import tr.com.hacettepe.tams.auth_service.dto.UserResponse;

import java.util.List;
import java.util.UUID;

/**
 * Admin-only operations for managing platform users.
 * All methods require ADMIN role at the controller level.
 */
public interface AdminUserService {

    /**
     * Creates a new user with a default password and sets mustChangePassword=true.
     * Admin accounts cannot be created through this method.
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * Returns all users except other ADMIN accounts.
     */
    List<UserResponse> listUsers();

    /**
     * Updates an existing user's username, email, and active status.
     * Role cannot be changed after creation.
     */
    UserResponse updateUser(UUID id, UpdateUserRequest request);

    /**
     * Permanently deletes a user and all associated refresh tokens.
     * Admin accounts cannot be deleted through this method.
     */
    void deleteUser(UUID id);

    /**
     * Resets a user's password to the system default and sets mustChangePassword=true.
     */
    void resetPassword(UUID id);
}
