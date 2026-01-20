package org.example.stego.dct;

import org.example.stego.StegoPayload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class DctSteganography {

    private static final int DELTA = 24;

    private static final int[][] POS = {
            {2, 3}, {3, 2}, {1, 4}, {4, 1}
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

                    if (Math.abs(c) < 2 * DELTA) c = (c >= 0 ? 2 * DELTA : -2 * DELTA);

                    block[i][j] = qimEmbed(c, bit, DELTA);
                }

                DctProcessor.inverseDCT(block);

                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        int yy = by + i;
                        int xx = bx + j;
                        if (yy < height && xx < width) {
                            newY[yy][xx] = block[i][j];
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

    // ---------------- QIM helpers ----------------
    private static int qimEmbed(int c, int bit, int delta) {
        int sign = (c >= 0) ? 1 : -1;
        int a = Math.abs(c);

        int k = (int) Math.round((double) a / delta);
        int base = k * delta;

        int target = base + (bit == 0 ? (delta / 4) : (3 * delta / 4));
        if (target == 0) target = delta;

        return sign * target;
    }

    private static int qimExtract(int c, int delta) {
        int a = Math.abs(c);
        int r = a % delta;
        return (r >= (delta / 2)) ? 1 : 0;
    }
}
