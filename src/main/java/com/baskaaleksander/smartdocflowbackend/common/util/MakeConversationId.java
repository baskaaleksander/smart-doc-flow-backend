package com.baskaaleksander.smartdocflowbackend.common.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MakeConversationId {
    public static String makeConversationId(String docId, UUID userId) {
        String key = docId + ":" + userId;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
