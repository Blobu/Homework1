package unitbv.devops.authenticationapi.dto.auth;

public record LoginResponse(
        boolean authenticated,
        UserResponse user
) {}
