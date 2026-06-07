package tr.com.hacettepe.tams.auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tr.com.hacettepe.tams.auth_service.domain.RefreshToken;
import tr.com.hacettepe.tams.auth_service.domain.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link RefreshToken} entities.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteAllByUser(User user);
}
