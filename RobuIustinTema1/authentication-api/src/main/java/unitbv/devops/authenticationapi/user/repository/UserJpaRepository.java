package unitbv.devops.authenticationapi.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unitbv.devops.authenticationapi.user.entity.User;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
