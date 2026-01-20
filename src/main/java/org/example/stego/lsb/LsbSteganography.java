package org.example.stego.lsb;

import org.example.stego.StegoPayload;

import java.awt.image.BufferedImage;

public class LsbSteganography {

    /**
     * Verilen görüntüye verilen mesajı gömer.
     * Dönüş: Yeni bir BufferedImage (orijinali değiştirmiyoruz).
     */
    public static BufferedImage embedMessage(BufferedImage coverImage, String message) {
        // 1) Payload byte dizisini üret
        byte[] payload = StegoPayload.buildPayload(message);

        // 2) Toplam bit sayısını hesapla
        int totalBits = payload.length * 8;

        int width = coverImage.getWidth();
        int height = coverImage.getHeight();
        int totalPixels = width * height;

        // Her pikselde 3 kanal (R,G,B) kullanacağız → 3 bit / piksel
        int capacityBits = totalPixels * 3;

        if (totalBits > capacityBits) {
            throw new IllegalArgumentException("Mesaj bu resme sığmıyor. " +
                    "Gerekli bit: " + totalBits + ", Kapasite: " + capacityBits);
        }

        // Orijinali bozmamak için kopya resim oluştur
        BufferedImage stegoImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                stegoImage.setRGB(x, y, coverImage.getRGB(x, y));
            }
        }

        int bitIndex = 0; // payload içindeki hangi bitteyiz?

        for (int y = 0; y < height && bitIndex < totalBits; y++) {
            for (int x = 0; x < width && bitIndex < totalBits; x++) {

                int rgb = stegoImage.getRGB(x, y);

                // R, G, B bileşenlerini ayır
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Her kanal için bir bit göm (sırasıyla R->G->B)
                r = setLsb(r, getPayloadBit(payload, bitIndex++));
                if (bitIndex < totalBits) {
                    g = setLsb(g, getPayloadBit(payload, bitIndex++));
                }
                if (bitIndex < totalBits) {
                    b = setLsb(b, getPayloadBit(payload, bitIndex++));
                }

                // Yeni rgb'yi oluştur
                int newRgb = (r << 16) | (g << 8) | b;
                stegoImage.setRGB(x, y, newRgb);
            }
        }

        return stegoImage;
    }

    /**
     * Bir byte dizisindeki belirli bitin değerini (0 veya 1) döndürür.
     * bitIndex: 0..(len*8-1)
     */
    private static int getPayloadBit(byte[] payload, int bitIndex) {
        int byteIndex = bitIndex / 8;
        int bitInByte = 7 - (bitIndex % 8); // MSB'den LSB'ye gitmek için 7-bit
        return (payload[byteIndex] >> bitInByte) & 1;
    }

    /**
     * Bir sayının LSB'sini verilen bit ile değiştirir.
     */
    private static int setLsb(int value, int bit) {
        value = (value & 0xFE) | (bit & 1); // son biti sıfırla, sonra bit ekle
        return value;
    }
}