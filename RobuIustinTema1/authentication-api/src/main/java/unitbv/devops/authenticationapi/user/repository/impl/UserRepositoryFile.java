package unitbv.devops.authenticationapi.user.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import unitbv.devops.authenticationapi.user.config.UserStorageProperties;
import unitbv.devops.authenticationapi.user.entity.User;
import unitbv.devops.authenticationapi.user.repository.UserRepository;

import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UserRepositoryFile implements UserRepository {

    private final Path storagePath;
    private final ObjectMapper mapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, User> byId = new HashMap<>();

    public UserRepositoryFile(ObjectMapper mapper, UserStorageProperties props) {
        this.mapper = mapper;
        this.storagePath = Paths.get(props.filePath());
        initStorage();
        loadAll();
    }

    private void initStorage() {
        try {
            Files.createDirectories(storagePath.getParent() == null ? Paths.get(".") : storagePath.getParent());
            if (Files.notExists(storagePath)) {
                Files.writeString(storagePath, "[]");
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize user storage: " + storagePath, e);
        }
    }

    private void loadAll() {
        lock.writeLock().lock();
        try {
            List<User> users = mapper.readValue(Files.readString(storagePath), new TypeReference<>() {});
            byId.clear();
            for (User u : users) {
                byId.put(u.getId(), u);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read users file: " + storagePath, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void persist() {
        lock.readLock().lock();
        try {
            List<User> users = new ArrayList<>(byId.values());
            Files.writeString(storagePath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(users),
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write users file: " + storagePath, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public User save(User user) {
        lock.writeLock().lock();
        try {
            if (user.getId() == null || user.getId().isBlank()) {
                user.setId(UUID.randomUUID().toString());
            }
            byId.put(user.getId(), user);
            persist();
            return user;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<User> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(byId.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        lock.readLock().lock();
        try {
            return byId.values().stream()
                    .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        lock.readLock().lock();
        try {
            return byId.values().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public List<User> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(byId.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void deleteById(String id) {
        lock.writeLock().lock();
        try {
            byId.remove(id);
            persist();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
