package com.takodu.api.shared.infrastructure.crypto;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM for secrets that must be stored, such as WhatsApp business tokens. The key comes from
 * {@code takodu.token-key} (the {@code TOKEN_ENCRYPTION_KEY} variable in production) and never touches the database.
 */
@ApplicationScoped
public class TokenCipher {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    /** @param base64Key 32 random bytes in base64, such as the output of {@code openssl rand -base64 32} */
    public TokenCipher(@ConfigProperty(name = "takodu.token-key") String base64Key) {
        var bytes = Base64.getDecoder().decode(base64Key.strip());
        if (bytes.length != 32) throw new IllegalStateException("takodu.token-key must be 32 bytes in base64");
        this.key = new SecretKeySpec(bytes, "AES");
    }

    /** @return the random IV followed by the ciphertext, in base64 */
    public String encrypt(String plain) {
        var iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            var sealed = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(IV_BYTES + sealed.length).put(iv).put(sealed)
                    .array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encrypting a token failed", e);
        }
    }

    /** @throws IllegalStateException when the value was not encrypted with this key or was altered */
    public String decrypt(String encrypted) {
        var bytes = Base64.getDecoder().decode(encrypted);
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES));
            return new String(cipher.doFinal(bytes, IV_BYTES, bytes.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Decrypting a token failed", e);
        }
    }
}
