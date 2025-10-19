package com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.TokenizerPort;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import org.springframework.stereotype.Component;

@Component
public class JtokkitTokenizerAdapter implements TokenizerPort {

    private final EncodingRegistry encodingRegistry = Encodings.newDefaultEncodingRegistry();
    private final Encoding encoding = encodingRegistry.getEncodingForModel(ModelType.TEXT_EMBEDDING_3_SMALL);


    @Override
    public int count(String rawText) {
        return encoding.countTokens(rawText);
    }
}
