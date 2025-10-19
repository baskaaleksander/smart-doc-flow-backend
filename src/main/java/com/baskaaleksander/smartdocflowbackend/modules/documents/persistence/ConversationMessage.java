package com.baskaaleksander.smartdocflowbackend.modules.documents.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.ConversationSide;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Table(name = "conversation_messages")
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationSide side;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private String fingerprint;

    @Column(nullable = false)
    private UUID conversationId;

    @Column(nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;
}
