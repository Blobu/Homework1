package unitbv.devops.authenticationapi.user.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Service;
import unitbv.devops.authenticationapi.config.JwtProperties;
import unitbv.devops.authenticationapi.user.entity.User;

import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final Algorithm hmacAlgorithm;
    private final JWTVerifier jwtVerifier;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(JwtProperties jwtProperties) {
        String jwtSecret = jwtProperties.getSecret();
        this.accessTokenExpiration = jwtProperties.getAccessTokenExpirationMs();
        this.refreshTokenExpiration = jwtProperties.getRefreshTokenExpirationMs();

        this.hmacAlgorithm = Algorithm.HMAC256(jwtSecret);
        this.jwtVerifier = JWT.require(hmacAlgorithm).build();
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return JWT.create()
                .withSubject(user.getUsername())
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .withClaim("roles", user.getRoles().stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .sign(hmacAlgorithm);
    }

    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return JWT.create()
                .withSubject(user.getUsername())
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(hmacAlgorithm);
    }

    public String extractUsername(String token) {
        try {
            return jwtVerifier.verify(token).getSubject();
        } catch (Exception e) {
            System.out.println("Error extracting username from token: " + e.getMessage());
            throw new RuntimeException("Invalid token: " + e.getMessage());
        }
    }

    public boolean isTokenValid(String token, org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername());
        } catch (Exception e) {
            System.out.println("Token validation error: " + e.getMessage());
            return false;
        }
    }
}