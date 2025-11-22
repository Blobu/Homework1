package unitbv.devops.authenticationapi.user.service;

import unitbv.devops.authenticationapi.dto.auth.*;

import java.util.List;
import java.util.Optional;

public interface UserService {

    // Metodele noi (Tema 6)
    Optional<AuthenticationResponse> register(RegisterRequest req);
    Optional<AuthenticationResponse> login(LoginRequest req);

    // Metoda veche (Tema 5)
    List<UserResponse> getAllUsers();
    Optional<AuthenticationResponse> refresh(TokenRefreshRequest req);
}