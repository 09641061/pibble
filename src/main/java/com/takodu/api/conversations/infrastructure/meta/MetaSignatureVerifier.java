package com.takodu.api.conversations.infrastructure.meta;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/** {@code X-Hub-Signature-256}: {@code sha256=} and the hex HMAC-SHA256 of the raw body with the app secret. */
public final class MetaSignatureVerifier {
    private static final String PREFIX = "sha256=";

    private final byte[] appSecret;

    public MetaSignatureVerifier(String appSecret) {
        this.appSecret = appSecret.getBytes(StandardCharsets.UTF_8);
    }

    /** Compares in constant time, so the response time reveals nothing about the expected signature. */
    public boolean verify(byte[] body, String header) {
        if (header == null || !header.startsWith(PREFIX)) return false;
        byte[] given;
        try {
            given = HexFormat.of().parseHex(header.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(sign(body), given);
    }

    /** @return the header Meta would send for the body */
    public String header(byte[] body) {
        return PREFIX + HexFormat.of().formatHex(sign(body));
    }

    private byte[] sign(byte[] body) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret, "HmacSHA256"));
            return mac.doFinal(body);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", e);
        }
    }
}
