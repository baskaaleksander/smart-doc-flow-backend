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
        UUID docId = UUID.randomUUID();

        Document doc = new Document.Builder()
                .id(docId)
                .filename("file.pdf")
                .mime("application/pdf")
                .size(2.0)
                .storageKey(docId + "_file.pdf")
                .pageSize(0)
                .status(DocumentStatus.UPLOADED)
                .build();

        DocumentResponse response = mapper.toResponse(doc);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(docId);
        assertThat(response.filename()).isEqualTo("file.pdf");
        assertThat(response.status()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(response.createdAt()).isEqualTo(doc.getCreatedAt());
    }

    @Test
    void toResponse_returnsNull_whenInputIsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}