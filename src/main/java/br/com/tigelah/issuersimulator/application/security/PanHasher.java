package br.com.tigelah.issuersimulator.application.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PanHasher {
    private PanHasher() {}

    public static String sha256(String pan) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            var bytes = md.digest(pan.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("failed_to_hash_pan", e);
        }
    }
}
