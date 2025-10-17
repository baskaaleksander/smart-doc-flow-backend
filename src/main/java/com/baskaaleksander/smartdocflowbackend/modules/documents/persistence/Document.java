package com.baskaaleksander.smartdocflowbackend.modules.documents.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.Review;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"owner", "review"})
@Table(name = "documents")
public class Document {

    @Id
    private UUID id;
    @Column(nullable = false)
    private String filename;
    @Column(nullable = false)
    private String mime;
    @Column(nullable = false)
    private double size;
    @Column(nullable = false)
    private String storageKey;
    @Column(nullable = false)
    private int pageSize;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(nullable = false)
    @CreationTimestamp
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private UserEntity owner;

    @OneToOne
    @JoinColumn(nullable = true)
    private Review review;


}
