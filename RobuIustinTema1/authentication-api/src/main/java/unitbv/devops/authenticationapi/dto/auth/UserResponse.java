package unitbv.devops.authenticationapi.dto.auth;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        String id,
        String username,
        String email,
        Set<String> roles,
        Instant createdAt,
        boolean enabled
) {}
