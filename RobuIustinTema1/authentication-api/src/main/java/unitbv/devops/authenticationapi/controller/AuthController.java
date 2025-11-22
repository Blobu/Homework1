package unitbv.devops.authenticationapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unitbv.devops.authenticationapi.dto.auth.*;
import unitbv.devops.authenticationapi.user.service.UserService;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService service;

    public AuthController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        return service.register(request)
                .<ResponseEntity<?>>map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new SimpleError("Username or email already in use")));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody @Valid LoginRequest request) {
        return service.login(request)
                // Pur și simplu returnăm răspunsul (care e deja AuthenticationResponse)
                .map(response -> ResponseEntity.ok(response))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/token")
    public ResponseEntity<AuthenticationResponse> refresh(@RequestBody @Valid TokenRefreshRequest request) {
        // Remove quotes from tokens if present
        String cleanAccessToken = request.accessToken().replaceAll("^\"|\"$", "");
        String cleanRefreshToken = request.refreshToken().replaceAll("^\"|\"$", "");

        TokenRefreshRequest cleanRequest = new TokenRefreshRequest(cleanAccessToken, cleanRefreshToken);

        return service.refresh(cleanRequest)
                .map(response -> ResponseEntity.ok(response))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        System.out.println("=== CONTROLLER: GET /api/auth/users called ===");
        List<UserResponse> users = service.getAllUsers();
        System.out.println("=== CONTROLLER: Returning " + users.size() + " users ===");
        return ResponseEntity.ok(users);
    }

    public record SimpleError(String error) {}
}
