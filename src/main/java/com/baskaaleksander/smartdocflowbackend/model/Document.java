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
    private String filename;
    private String mime;
    private double size;
    private String storageKey;
    private int pageSize;
    @Enumerated(EnumType.ORDINAL)
    private DocumentStatus status;
    private LocalDateTime createdAt;

    @ManyToOne
    private User owner;
}
