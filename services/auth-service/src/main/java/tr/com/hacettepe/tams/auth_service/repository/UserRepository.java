package tr.com.hacettepe.tams.auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tr.com.hacettepe.tams.auth_service.domain.Role;
import tr.com.hacettepe.tams.auth_service.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link User} entities.
 * All finders used in the authentication flow are declared here
 * so that no raw JPQL is scattered across service classes.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.role <> :role ORDER BY LOWER(u.username) ASC")
    List<User> findAllByRoleNotSortedByUsernameAsc(@Param("role") Role role);

    /**
     * Used during login to allow the user to sign in with either
     * their e-mail address or their username.
     */
    Optional<User> findByEmailOrUsername(String email, String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByStudentNumber(String studentNumber);
}
