package unitbv.devops.authenticationapi.user.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import unitbv.devops.authenticationapi.token.Token;
import unitbv.devops.authenticationapi.token.TokenRepository;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;

    public JwtAuthFilter(JwtService jwtService,
                         UserDetailsService userDetailsService,
                         TokenRepository tokenRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenRepository = tokenRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String requestURI = request.getRequestURI();

        System.out.println("=== JWT FILTER - Checking: " + requestURI);

        // Skip filter for ALL public endpoints
        if (isPublicEndpoint(requestURI)) {
            System.out.println("✅ Skipping JWT filter for public endpoint: " + requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ Missing or invalid Authorization header for: " + requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing Authorization header");
            return;
        }

        // Extract and clean token
        String jwt = authHeader.substring(7).replaceAll("^\"|\"$", "");

        try {
            String username = jwtService.extractUsername(jwt);
            System.out.println("Username from token: " + username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Check token validity
                boolean isTokenValid = jwtService.isTokenValid(jwt, userDetails);
                System.out.println("Token JWT valid: " + isTokenValid);

                // STRICT BLACKLIST CHECK using JpaRepository
                Optional<Token> tokenInDb = tokenRepository.findByAccessToken(jwt);
                if (tokenInDb.isPresent()) {
                    Token token = tokenInDb.get();
                    boolean isBlacklisted = token.isBlacklisted();
                    System.out.println("Token found in DB - ID: " + token.getId() + ", Blacklisted: " + isBlacklisted);

                    if (isBlacklisted) {
                        System.out.println("❌ ACCESS DENIED: Token is blacklisted!");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Token has been revoked");
                        return;
                    }
                } else {
                    System.out.println("❌ ACCESS DENIED: Token not found in database!");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token");
                    return;
                }

                if (isTokenValid) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("✅ ACCESS GRANTED for user: " + username);
                } else {
                    System.out.println("❌ ACCESS DENIED: Token JWT invalid");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token");
                    return;
                }
            }
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            System.out.println("❌ JWT Filter Exception: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token: " + e.getMessage());
        }
    }

    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.equals("/") ||
                requestURI.equals("/api/health") ||
                requestURI.startsWith("/swagger-ui") ||
                requestURI.startsWith("/v3/api-docs") ||
                requestURI.startsWith("/webjars") ||
                requestURI.startsWith("/swagger-resources") ||
                requestURI.startsWith("/configuration") ||
                requestURI.equals("/api/auth/register") ||
                requestURI.equals("/api/auth/login") ||
                requestURI.equals("/api/auth/token");
    }
}