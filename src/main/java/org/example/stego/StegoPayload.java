package org.example.stego;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

public class StegoPayload {

    private static final byte[] MAGIC = new byte[]{'S','T','E','G'}; // 4 byte

    public static byte[] buildPayload(String message) {
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(
                MAGIC.length + 4 + msgBytes.length + 4
        );
        buffer.put(MAGIC);
        buffer.putInt(msgBytes.length);
        buffer.put(msgBytes);

        CRC32 crc = new CRC32();
        crc.update(msgBytes);
        buffer.putInt((int) crc.getValue());

        return buffer.array();
    }

    public static String tryParseFromStream(byte[] arr) {
        if (arr.length < 8) return null;

        if (arr.length >= 4) {
            if (!Arrays.equals(Arrays.copyOfRange(arr, 0, 4), MAGIC)) return null;
        }

        int len;
        try {
            len = parseLength(arr);
        } catch (Exception e) {
            return null;
        }

        if (len < 0 || len > 5_000_000) return null;

        int total = 4 /*MAGIC*/ + 4 /*len*/ + len + 4 /*crc*/;
        if (arr.length < total) return null;

        try {
            return parsePayloadBytes(Arrays.copyOf(arr, total));
        } catch (Exception e) {
            return null;
        }
    }

    public static String parsePayloadBytes(byte[] data) {
        if (!Arrays.equals(Arrays.copyOfRange(data, 0, 4), MAGIC)) {
            throw new IllegalArgumentException("Stego MAGIC bulunamadı");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.position(4);
        int len = buffer.getInt();

        byte[] msgBytes = new byte[len];
        buffer.get(msgBytes);

        int crcRead = buffer.getInt();

        CRC32 crc = new CRC32();
        crc.update(msgBytes);

        if ((int) crc.getValue() != crcRead) {
            throw new IllegalStateException("CRC doğrulaması başarısız");
        }

        return new String(msgBytes, StandardCharsets.UTF_8);
    }

    public static int parseLength(byte[] data) {
        if (data.length < 8) return -1;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.position(4);
        return buffer.getInt();
    }

    private static int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }


}
