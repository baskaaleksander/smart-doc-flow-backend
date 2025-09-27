package com.baskaaleksander.smartdocflowbackend.modules.documents.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.ConversationSide;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationSide side;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private UUID conversationId;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;
}
