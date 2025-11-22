package unitbv.devops.authenticationapi.user.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import unitbv.devops.authenticationapi.dto.auth.*;
import unitbv.devops.authenticationapi.user.service.JwtService;
import unitbv.devops.authenticationapi.token.Token;
import unitbv.devops.authenticationapi.token.TokenRepository;
import unitbv.devops.authenticationapi.user.entity.Role;
import unitbv.devops.authenticationapi.user.entity.User;
import unitbv.devops.authenticationapi.user.mapper.UserMapper; // <-- Importă Mapper-ul static
import unitbv.devops.authenticationapi.user.repository.UserRepository;
import unitbv.devops.authenticationapi.user.service.UserService;

import java.time.Instant;
import java.util.List; // <-- Asigură-te că e java.util.List
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors; // <-- Importă Collectors

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;

    public UserServiceImpl(UserRepository users,
                           PasswordEncoder encoder,
                           JwtService jwtService,
                           TokenRepository tokenRepository) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.tokenRepository = tokenRepository;
    }

    // --- Metoda REGISTER (Cu JWT) ---
    @Override
    public Optional<AuthenticationResponse> register(RegisterRequest req) {
        if (users.existsByUsername(req.username()) || users.existsByEmail(req.email())) {
            return Optional.empty();
        }

        User u = User.builder()
                .id(java.util.UUID.randomUUID().toString())
                .username(req.username())
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .roles(Set.of(Role.USER))
                .createdAt(Instant.now())
                .enabled(true)
                .build();
        u = users.save(u);

        String accessToken = jwtService.generateAccessToken(u);
        String refreshToken = jwtService.generateRefreshToken(u);
        saveUserToken(u, accessToken, refreshToken);

        return Optional.of(new AuthenticationResponse(accessToken, refreshToken));
    }


    // --- Metoda LOGIN (Cu JWT) ---
    @Override
    public Optional<AuthenticationResponse> login(LoginRequest req) {
        Optional<User> found = users.findByUsername(req.usernameOrEmail());
        if (found.isEmpty()) {
            found = users.findByEmail(req.usernameOrEmail());
        }
        if (found.isEmpty()) {
            return Optional.empty();
        }

        User u = found.get();
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            return Optional.empty();
        }

        String accessToken = jwtService.generateAccessToken(u);
        String refreshToken = jwtService.generateRefreshToken(u);
        saveUserToken(u, accessToken, refreshToken);

        return Optional.of(new AuthenticationResponse(accessToken, refreshToken));
    }

    // --- Metoda ta GETALLUSERS (Din Lab 5) ---
    // Aceasta este metoda pe care am omis-o din greșeală
    @Override
    public List<UserResponse> getAllUsers() {
        System.out.println("=== SERVICE: getAllUsers() called ===");

        try {
            // 1. Get all user entities
            List<User> userEntities = users.findAll();
            System.out.println("=== SERVICE: Found " + userEntities.size() + " users in database ===");

            // 2. Map to responses
            List<UserResponse> responses = userEntities.stream()
                    .map(user -> {
                        System.out.println("=== SERVICE: Mapping user: " + user.getUsername() + " ===");
                        return UserMapper.toResponse(user);
                    })
                    .collect(Collectors.toList());

            System.out.println("=== SERVICE: Returning " + responses.size() + " user responses ===");
            return responses;
        } catch (Exception e) {
            System.out.println("=== SERVICE: ERROR in getAllUsers: " + e.getMessage() + " ===");
            e.printStackTrace();
            return List.of(); // Return empty list on error
        }
    }

    // --- Metoda ajutătoare pentru a salva token-urile ---
    private void saveUserToken(User user, String accessToken, String refreshToken) {
        try {
            // Clean tokens before saving
            String cleanAccessToken = accessToken.replaceAll("^\"|\"$", "");
            String cleanRefreshToken = refreshToken.replaceAll("^\"|\"$", "");

            Token token = Token.builder()
                    .user(user)
                    .accessToken(cleanAccessToken)
                    .refreshToken(cleanRefreshToken)
                    .blacklisted(false)
                    .createdAt(Instant.now())
                    .build();

            Token savedToken = tokenRepository.save(token);
            System.out.println("✅ Saved new token to DB - ID: " + savedToken.getId());
        } catch (Exception e) {
            System.out.println("❌ Error saving token: " + e.getMessage());
            throw e;
        }
    }


    @Override
    public Optional<AuthenticationResponse> refresh(TokenRefreshRequest req) {
        try {
            // Clean tokens from quotes
            String accessToken = req.accessToken().replaceAll("^\"|\"$", "");
            String refreshToken = req.refreshToken().replaceAll("^\"|\"$", "");

            System.out.println("=== TOKEN REFRESH STARTED ===");

            // 1. Find token by refresh token using JpaRepository method
            Optional<Token> refreshTokenOpt = tokenRepository.findByRefreshToken(refreshToken);
            if (refreshTokenOpt.isEmpty()) {
                System.out.println("❌ Refresh token not found in database");
                return Optional.empty();
            }

            Token tokenEntity = refreshTokenOpt.get();
            System.out.println("✅ Found token in DB - ID: " + tokenEntity.getId() + ", Blacklisted: " + tokenEntity.isBlacklisted());

            // 2. Verify the access token matches
            if (!tokenEntity.getAccessToken().equals(accessToken)) {
                System.out.println("❌ Access token mismatch");
                return Optional.empty();
            }

            // 3. Check if already blacklisted
            if (tokenEntity.isBlacklisted()) {
                System.out.println("❌ Token is already blacklisted");
                return Optional.empty();
            }

            // 4. Verify user
            User user = tokenEntity.getUser();
            if (user == null || !user.isEnabled()) {
                System.out.println("❌ User is null or disabled");
                return Optional.empty();
            }

            System.out.println("✅ User valid: " + user.getUsername());

            // 5. BLACKLIST THE OLD TOKEN using JpaRepository save
            tokenEntity.setBlacklisted(true);
            Token savedBlacklistedToken = tokenRepository.save(tokenEntity);
            System.out.println("✅ Successfully blacklisted token ID: " + savedBlacklistedToken.getId());

            // 6. Generate new tokens
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            System.out.println("✅ Generated new tokens");

            // 7. Save new tokens using JpaRepository
            saveUserToken(user, newAccessToken, newRefreshToken);
            System.out.println("✅ Saved new tokens to database");

            return Optional.of(new AuthenticationResponse(newAccessToken, newRefreshToken));

        } catch (Exception e) {
            System.out.println("❌ Error during token refresh: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
}