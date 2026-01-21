package org.example.stego.dct;

import org.example.stego.StegoPayload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class DctSteganography {

    // DELTA değerini düşürdük - daha az distorsiyon
    private static final int DELTA = 24;

    // Minimum eşik değeri - çok küçük katsayıları kullanmayalım
    private static final int MIN_COEFF_THRESHOLD = 12;

    // Daha dengeli pozisyonlar - orta frekans bölgesinde
    private static final int[][] POS = {
            {2, 3}, {3, 2}, {4, 1}, {1, 4},
            {2, 4}, {4, 2}, {3, 3},{1,1}
    };

    public static BufferedImage embedMessageDct(BufferedImage image, String message) throws Exception {

        int width = image.getWidth();
        int height = image.getHeight();

        double[][] Y = ColorSpaceUtils.getYChannel(image);

        byte[] payload = StegoPayload.buildPayload(message);
        BitInputStream bits = new BitInputStream(payload);

        int blocksX = (width + 7) / 8;
        int blocksY = (height + 7) / 8;

        int capacityBits = blocksX * blocksY;
        int neededBits = payload.length * 8;

        if (neededBits > capacityBits) {
            throw new IllegalArgumentException(
                    "Mesaj çok uzun. Gerekli bit=" + neededBits +
                            ", kapasite bit=" + capacityBits + " (DCT 1bit/blok)"
            );
        }

        double[][] newY = new double[height][width];

        int posIdx = 0;

        for (int by = 0; by < height; by += 8) {
            for (int bx = 0; bx < width; bx += 8) {

                double[][] block = DctProcessor.extract8x8(Y, by, bx);

                DctProcessor.forwardDCT(block);

                if (bits.hasNextBit()) {
                    int bit = bits.nextBit();

                    int i = POS[posIdx][0];
                    int j = POS[posIdx][1];
                    posIdx = (posIdx + 1) % POS.length;

                    int c = (int) Math.round(block[i][j]);

                    // Eğer katsayı çok küçükse, güvenilir embedding için artıralım
                    if (Math.abs(c) < MIN_COEFF_THRESHOLD) {
                        c = (c >= 0 ? MIN_COEFF_THRESHOLD : -MIN_COEFF_THRESHOLD);
                    }

                    // Daha yumuşak QIM embedding
                    block[i][j] = qimEmbed(c, bit, DELTA);
                }

                DctProcessor.inverseDCT(block);

                // Değerleri clamp edelim (0-255 aralığında tutalım)
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        int yy = by + i;
                        int xx = bx + j;
                        if (yy < height && xx < width) {
                            // Clamp to valid range
                            newY[yy][xx] = Math.max(0, Math.min(255, block[i][j]));
                        }
                    }
                }
            }
        }

        if (bits.hasNextBit()) {
            throw new RuntimeException("Embed tamamlanamadı: beklenmeyen kapasite yetmedi.");
        }

        return ColorSpaceUtils.replaceYChannel(image, newY);
    }

    public static String extractMessageDct(BufferedImage stegoImage) throws Exception {

        int width = stegoImage.getWidth();
        int height = stegoImage.getHeight();

        double[][] Y = ColorSpaceUtils.getYChannel(stegoImage);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int cur = 0;
        int count = 0;

        int posIdx = 0;

        for (int by = 0; by < height; by += 8) {
            for (int bx = 0; bx < width; bx += 8) {

                double[][] block = DctProcessor.extract8x8(Y, by, bx);
                DctProcessor.forwardDCT(block);

                int i = POS[posIdx][0];
                int j = POS[posIdx][1];
                posIdx = (posIdx + 1) % POS.length;

                int c = (int) Math.round(block[i][j]);

                int bit = qimExtract(c, DELTA);

                cur = (cur << 1) | bit;
                count++;

                if (count == 8) {
                    baos.write(cur);
                    cur = 0;
                    count = 0;

                    byte[] arr = baos.toByteArray();
                    String msg = StegoPayload.tryParseFromStream(arr);
                    if (msg != null) return msg;
                }
            }
        }

        throw new RuntimeException("DCT stego mesajı bulunamadı (QIM).");
    }

    // ---------------- QIM helpers (iyileştirilmiş) ----------------
    private static int qimEmbed(int c, int bit, int delta) {
        int sign = (c >= 0) ? 1 : -1;
        int a = Math.abs(c);

        // Daha hassas quantization
        int k = a / delta;
        int base = k * delta;

        // Bit 0 için delta/4, bit 1 için 3*delta/4
        int offset = (bit == 0) ? (delta / 4) : (3 * delta / 4);
        int target = base + offset;

        // Sıfır kontrolü
        if (target == 0) {
            target = (bit == 0) ? (delta / 4) : (3 * delta / 4);
        }

        return sign * target;
    }

    private static int qimExtract(int c, int delta) {
        int a = Math.abs(c);
        int r = a % delta;

        // Merkez noktası delta/2
        return (r >= (delta / 2)) ? 1 : 0;
    }
}