package org.example.stego.lsb;

import org.example.stego.StegoPayload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class LsbExtractor {

    /**
     * LSB ile gömülü mesajı verilen görüntüden çıkarır.
     */
    public static String extractMessage(BufferedImage stegoImage) {
        int width = stegoImage.getWidth();
        int height = stegoImage.getHeight();

        // Önce yeterince bit okuyup tüm payload'ı çıkarmamız gerekiyor.
        // Basit yöntem: tüm resmi okuyup byte dizisi oluştur, sonra parse etmeyi dene.
        // (Daha optimize versiyonunda önce sadece header oku, length'e göre devam ederiz.)

        ByteArrayOutputStream bitStream = new ByteArrayOutputStream();

        // Bitleri biriktirmek için geçici byte
        int currentByte = 0;
        int bitsFilled = 0;

        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int rgb = stegoImage.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // R
                currentByte = (currentByte << 1) | (r & 1);
                bitsFilled++;
                if (bitsFilled == 8) {
                    bitStream.write(currentByte);
                    currentByte = 0;
                    bitsFilled = 0;
                }

                // G
                currentByte = (currentByte << 1) | (g & 1);
                bitsFilled++;
                if (bitsFilled == 8) {
                    bitStream.write(currentByte);
                    currentByte = 0;
                    bitsFilled = 0;
                }

                // B
                currentByte = (currentByte << 1) | (b & 1);
                bitsFilled++;
                if (bitsFilled == 8) {
                    bitStream.write(currentByte);
                    currentByte = 0;
                    bitsFilled = 0;
                }
            }
        }

        byte[] allBytes = bitStream.toByteArray();

        // allBytes içinde hem header hem mesaj var (ve belki fazlası).
        // StegoPayload.parsePayloadBytes sadece gerekli kısmı kullanacak.
        return StegoPayload.parsePayloadBytes(allBytes);
    }
}