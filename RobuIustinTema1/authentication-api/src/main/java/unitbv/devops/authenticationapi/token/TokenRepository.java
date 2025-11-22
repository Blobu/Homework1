package unitbv.devops.authenticationapi.token;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    // Spring Data JPA will automatically generate these queries
    Optional<Token> findByAccessToken(String accessToken);

    Optional<Token> findByRefreshToken(String refreshToken);

    // Check if a token exists and is blacklisted
    default boolean isAccessTokenBlacklisted(String accessToken) {
        return findByAccessToken(accessToken)
                .map(Token::isBlacklisted)
                .orElse(false);
    }
}