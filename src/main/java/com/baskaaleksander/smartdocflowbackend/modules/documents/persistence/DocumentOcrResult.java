package com.baskaaleksander.smartdocflowbackend.modules.documents.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_ocr_results")
@Data @NoArgsConstructor @AllArgsConstructor
public class DocumentOcrResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String storageKey;

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Document document;

    @Version
    private int version = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
