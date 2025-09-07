package com.baskaaleksander.smartdocflowbackend.model;

import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
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
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    private User owner;
}
