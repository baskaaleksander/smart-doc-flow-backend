package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

public interface ConversationEncryptionServicePort {
    String encrypt(String plainText);
    String decrypt(String cipherText);
    String fingerprint(String message);
}
