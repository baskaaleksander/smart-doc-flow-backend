package com.baskaaleksander.smartdocflowbackend.modules.users.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.Role;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Getter @Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Column(nullable = false)
    private Set<Role> roles;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Document> documents;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return this.id == ((User) o).getId();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
