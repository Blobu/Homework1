package unitbv.devops.authenticationapi.token;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import unitbv.devops.authenticationapi.user.entity.User;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tokens")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "token_seq")
    @SequenceGenerator(name = "token_seq", sequenceName = "tokens_seq", allocationSize = 1)
    private Long id;

    @Column(name = "access_token", length = 1024, nullable = false)
    private String accessToken;

    @Column(name = "refresh_token", length = 1024, nullable = false)
    private String refreshToken;

    @Builder.Default
    @Column(nullable = false)
    private boolean blacklisted = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}