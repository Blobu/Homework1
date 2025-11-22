package unitbv.devops.authenticationapi.user.repository;

import java.util.List;
import java.util.Optional;

import unitbv.devops.authenticationapi.user.entity.User;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(String id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findAll();

    void deleteById(String id);
}
