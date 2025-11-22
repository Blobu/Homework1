package unitbv.devops.authenticationapi.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import unitbv.devops.authenticationapi.token.Token;

import java.util.Collection;
import java.util.List;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // mappedBy = "user" se referă la câmpul 'user' din clasa Token
    @OneToMany(mappedBy = "user")
    private List<Token> tokens;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean enabled;

    // --- Implementarea metodelor UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Transformă lista ta de 'Role' (Enum) într-o listă de 'GrantedAuthority'
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        // Returnează hash-ul parolei, NU parola simplă
        return passwordHash;
    }

    @Override
    public String getUsername() {
        // Returnează username-ul
        return username;
    }

// Pentru acest laborator, putem lăsa acestea ca 'true'
// Câmpul 'enabled' este singurul pe care îl folosim

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Folosim câmpul 'enabled' din entitatea ta
        return this.enabled;
    }
}
