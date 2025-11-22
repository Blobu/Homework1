package unitbv.devops.authenticationapi.dto.auth;

// Un DTO (record) simplu pentru a returna token-urile
public record AuthenticationResponse(
        String accessToken,
        String refreshToken
) {}