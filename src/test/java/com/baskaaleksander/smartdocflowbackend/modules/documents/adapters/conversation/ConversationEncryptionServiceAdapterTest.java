package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.conversation;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.ConversationEncryptionServicePort;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationEncryptionServiceAdapterTest {

    private ConversationEncryptionServicePort newService() {
        String aesSecret = Base64.getEncoder().encodeToString("1234567890123456".getBytes(StandardCharsets.UTF_8));
        String hmacSecret = Base64.getEncoder().encodeToString("abcdefghijklmnop".getBytes(StandardCharsets.UTF_8));
        return new ConversationEncryptionServiceAdapter(aesSecret, hmacSecret);
    }

    @Test
    void encryptDecrypt_roundtrip() {
        ConversationEncryptionServicePort svc = newService();
        String plain = "hello world ąęćźż";

        String cipher = svc.encrypt(plain);
        String out = svc.decrypt(cipher);

        assertThat(out).isEqualTo(plain);
        assertThat(cipher).isNotEqualTo(plain);
    }

    @Test
    void encrypt_usesRandomIv_producesDifferentCiphertexts() {
        ConversationEncryptionServicePort svc = newService();
        String plain = "same input";

        String c1 = svc.encrypt(plain);
        String c2 = svc.encrypt(plain);

        assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    void fingerprint_isDeterministic_andDifferentForDifferentMessages() {
        ConversationEncryptionServicePort svc = newService();

        String f1a = svc.fingerprint("msg1");
        String f1b = svc.fingerprint("msg1");
        String f2 = svc.fingerprint("msg2");

        assertThat(f1a).isEqualTo(f1b);
        assertThat(f1a).isNotEqualTo(f2);
    }

    @Test
    void constructor_rejectsInvalidAesKeyLength() {
        String badAes = Base64.getEncoder().encodeToString("short-key-15b".getBytes(StandardCharsets.UTF_8));
        String hmac = Base64.getEncoder().encodeToString("abcdefghijklmnop".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new ConversationEncryptionServiceAdapter(badAes, hmac))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AES key must be 16/24/32 bytes");
    }

    @Test
    void decrypt_withInvalidCipher_throwsRuntimeException() {
        ConversationEncryptionServicePort svc = newService();

        assertThatThrownBy(() -> svc.decrypt("not-base64"))
                .isInstanceOf(RuntimeException.class);
    }
}