package com.baskaaleksander.smartdocflowbackend.modules.documents.domain;

import java.util.HashMap;
import java.util.List;

public record OpenAiCompletionsRequest(
        String model,
        List<HashMap<String, Object>> messages
) {
}
