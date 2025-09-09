package com.baskaaleksander.smartdocflowbackend.model;

import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
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
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToOne
    @JoinColumn(nullable = true)
    private Review review;

}
