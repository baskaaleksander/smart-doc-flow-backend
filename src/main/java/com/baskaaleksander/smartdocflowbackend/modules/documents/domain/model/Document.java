package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Document {
    private final UUID id;
    private final String filename;
    private final String mime;
    private final double size;
    private final String storageKey;
    private final int pageSize;
    private final DocumentStatus status;
    private final Instant createdAt;
    private final DocumentUserBasic owner;
    private final DocumentReviewBasic review;

    public Document(Builder builder) {
        this.id = builder.id;
        this.filename = builder.filename;
        this.mime = builder.mime;
        this.size = builder.size;
        this.storageKey = builder.storageKey;
        this.pageSize = builder.pageSize;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.owner = builder.owner;
        this.review = builder.review;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String filename;
        private String mime;
        private Double size;
        private String storageKey;
        private Integer pageSize;
        private DocumentStatus status;
        private Instant createdAt = Instant.now();
        private DocumentUserBasic owner;
        private DocumentReviewBasic review;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder mime(String mime) {
            this.mime = mime;
            return this;
        }

        public Builder size(double size) {
            this.size = size;
            return this;
        }

        public Builder storageKey(String storageKey) {
            this.storageKey = storageKey;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder status(DocumentStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder owner(DocumentUserBasic owner) {
            this.owner = owner;
            return this;
        }

        public Builder review(DocumentReviewBasic review) {
            this.review = review;
            return this;
        }

        public Document build() {
            if (filename == null || mime == null || size == null || storageKey == null || pageSize == null || status == null) {
                throw new IllegalArgumentException("Missing required fields: filename, mime, size, storageKey, pageSize or status");
            }
            return new Document(this);
        }
    }
}


