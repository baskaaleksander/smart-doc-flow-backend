package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentApiMapperTest {

    private final DocumentApiMapper mapper = Mappers.getMapper(DocumentApiMapper.class);

    @Test
    void toResponse_mapsAllBasicFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        Document document = new Document();
        document.setId(id);
        document.setFilename("example.pdf");
        document.setStatus(DocumentStatus.PROCESSED);
        document.setCreatedAt(Instant.now());

        DocumentResponse response = mapper.toResponse(document);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.filename()).isEqualTo("example.pdf");
        assertThat(response.status()).isEqualTo(DocumentStatus.PROCESSED);
        assertThat(response.createdAt()).isEqualTo(document.getCreatedAt());
    }

    @Test
    void toResponse_returnsNull_whenInputIsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}