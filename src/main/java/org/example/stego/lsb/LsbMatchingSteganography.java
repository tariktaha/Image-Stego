package org.example.stego.lsb;


import org.example.stego.StegoPayload;

import java.awt.image.BufferedImage;
import java.util.Random;

public class LsbMatchingSteganography {

    /**
     * LSB Matching (±1) yöntemi ile mesaj gömme.
     */
    public static BufferedImage embedMessageMatching(BufferedImage coverImage, String message) {
        byte[] payload = StegoPayload.buildPayload(message);
        int totalBits = payload.length * 8;

        int width = coverImage.getWidth();
        int height = coverImage.getHeight();
        int totalPixels = width * height;

        int capacityBits = totalPixels * 3; // R,G,B

        if (totalBits > capacityBits) {
            throw new IllegalArgumentException("Mesaj bu resme sığmıyor. " +
                    "Gerekli bit: " + totalBits + ", Kapasite: " + capacityBits);
        }

        // Orijinal görüntüyü kopyala
        BufferedImage stegoImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                stegoImage.setRGB(x, y, coverImage.getRGB(x, y));
            }
        }

        Random random = new Random(); // basit random, key'siz versiyon
        int bitIndex = 0;

        for (int y = 0; y < height && bitIndex < totalBits; y++) {
            for (int x = 0; x < width && bitIndex < totalBits; x++) {

                int rgb = stegoImage.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // R kanalı
                int bit = getPayloadBit(payload, bitIndex++);
                r = applyLsbMatching(r, bit, random);

                // G kanalı
                if (bitIndex < totalBits) {
                    bit = getPayloadBit(payload, bitIndex++);
                    g = applyLsbMatching(g, bit, random);
                }

                // B kanalı
                if (bitIndex < totalBits) {
                    bit = getPayloadBit(payload, bitIndex++);
                    b = applyLsbMatching(b, bit, random);
                }

                int newRgb = (r << 16) | (g << 8) | b;
                stegoImage.setRGB(x, y, newRgb);
            }
        }

        return stegoImage;
    }

    /**
     * payload içindeki belirli bit'i döner (0 veya 1).
     */
    private static int getPayloadBit(byte[] payload, int bitIndex) {
        int byteIndex = bitIndex / 8;
        int bitInByte = 7 - (bitIndex % 8);
        return (payload[byteIndex] >> bitInByte) & 1;
    }

    /**
     * LSB Matching (±1): Eğer value'nun LSB'si zaten hedef bite eşitse dokunma.
     * Değilse, value'yu ±1 ile değiştirerek LSB'yi değiştir.
     */
    private static int applyLsbMatching(int value, int targetBit, Random random) {
        int currentLsb = value & 1;
        if (currentLsb == targetBit) {
            // Zaten istediğimiz bit, dokunmaya gerek yok
            return value;
        }

        // LSB != targetBit ise ±1 ile değişiklik yapacağız.
        if (value == 0) {
            // 0 ise -1 yapamayız, sadece +1
            return 1;
        } else if (value == 255) {
            // 255 ise +1 yapamayız, sadece -1
            return 254;
        } else {
            // 1..254 arası ise rastgele +1 veya -1
            if (random.nextBoolean()) {
                return value + 1;
            } else {
                return value - 1;
            }
        }
    }
}