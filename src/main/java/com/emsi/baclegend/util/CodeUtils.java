package com.emsi.baclegend.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CodeUtils {

    /**
     * Compresse une adresse IP (ex: 192.168.1.15) et un port (ex: 5000)
     * en une chaîne courte de caractères (Base64 URL-safe).
     * Format:
     * - Si IPv4: 4 bytes IP + 2 bytes Port = 6 bytes. Base64(6) ~= 8 chars.
     * - Si IPv6 ou autre: Fallback sur l'encodage classique plus long.
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
                return encoded;
            }
        } catch (Exception ignored) {
            // Fallback
        }
        // Fallback: encode full string
        return Base64.getUrlEncoder().withoutPadding().encodeToString((ip + ":" + port).getBytes());
    }

    /**
     * Décompresse un code court (ex: "wKgBexNM") en "IP:Port".
     */
    public static String decompress(String code) {
        try {
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
