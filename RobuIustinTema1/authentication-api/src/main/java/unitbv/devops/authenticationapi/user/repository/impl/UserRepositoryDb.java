package unitbv.devops.authenticationapi.user.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import unitbv.devops.authenticationapi.user.entity.User;
import unitbv.devops.authenticationapi.user.repository.UserJpaRepository;
import unitbv.devops.authenticationapi.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
@RequiredArgsConstructor
public class UserRepositoryDb implements UserRepository {

    private final UserJpaRepository jpaRepo;

    @Override
    public User save(User user) {
        return jpaRepo.save(user);
    }

    @Override
    public Optional<User> findById(String id) {
        return jpaRepo.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepo.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepo.findByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepo.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepo.existsByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return jpaRepo.findAll();
    }

    @Override
    public void deleteById(String id) {
        jpaRepo.deleteById(id);
    }
}
