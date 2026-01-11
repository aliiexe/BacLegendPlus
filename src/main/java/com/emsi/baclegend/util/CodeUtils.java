package com.emsi.baclegend.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;

public class CodeUtils {

    private static final Random random = new Random();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Exclude confusing chars like 0, O, I, 1

    /**
     * Compresse une adresse IP (ex: 192.168.1.15) et un port (ex: 5000)
     * en une chaîne courte de caractères (Base64 URL-safe) avec un suffixe aléatoire unique.
     * Format:
     * - Si IPv4: 4 bytes IP + 2 bytes Port = 6 bytes. Base64(6) ~= 8 chars + 4 random chars = 12 chars total.
     * - Si IPv6 ou autre: Fallback sur l'encodage classique plus long + 4 random chars.
     */
    public static String compress(String ip, int port) {
        try {
            // Check if IPv4 (basic check)
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                byte[] data = new byte[6];
                for (int i = 0; i < 4; i++) {
                    int val = Integer.parseInt(parts[i]);
                    data[i] = (byte) val;
                }
                // Little Endian/Big Endian doesn't matter as long as consistent
                data[4] = (byte) ((port >> 8) & 0xFF);
                data[5] = (byte) (port & 0xFF);

                // Encode Base64 without padding for brevity
                String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(data);
                // Add random 4-character suffix to make each code unique
                String randomSuffix = generateRandomSuffix();
                return encoded + randomSuffix;
            }
        } catch (Exception ignored) {
            // Fallback
        }
        // Fallback: encode full string + random suffix
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString((ip + ":" + port).getBytes());
        String randomSuffix = generateRandomSuffix();
        return encoded + randomSuffix;
    }

    /**
     * Generates a random 4-character suffix to make codes unique
     */
    private static String generateRandomSuffix() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Décompresse un code court (ex: "wKgBexNMABCD") en "IP:Port".
     * Removes the random 4-character suffix before decoding.
     */
    public static String decompress(String code) {
        try {
            // Remove the last 4 characters (random suffix) before decoding
            if (code.length() >= 4) {
                code = code.substring(0, code.length() - 4);
            }
            byte[] data = Base64.getUrlDecoder().decode(code);

            // If 6 bytes, it's our IPv4 condensed format
            if (data.length == 6) {
                StringBuilder ip = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    // Convert signed byte to unsigned int 0-255
                    ip.append(data[i] & 0xFF);
                    if (i < 3)
                        ip.append(".");
                }

                int port = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
                return ip.toString() + ":" + port;
            }

            // Else assume full string
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
