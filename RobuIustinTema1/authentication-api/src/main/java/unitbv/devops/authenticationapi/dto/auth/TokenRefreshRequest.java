package unitbv.devops.authenticationapi.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @NotBlank
        String accessToken,

        @NotBlank
        String refreshToken
) {}