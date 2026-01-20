package org.example.stego.dwt;

import org.example.stego.StegoPayload;
import org.example.stego.dct.BitInputStream;
import org.example.stego.dct.ColorSpaceUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class DwtSteganography {

    private static final int Q = 24;

    private static double qimEmbed(double c, int bit) {
        if (c == 0) c = 2;
        double sign = (c < 0) ? -1.0 : 1.0;
        double a = Math.abs(c);

        double base = Math.floor(a / Q) * Q;
        double target = base + (bit == 0 ? (Q * 0.25) : (Q * 0.75));
        return sign * target;
    }

    private static int qimExtract(double c) {
        double a = Math.abs(c);
        double r = a % Q;
        return (r >= (Q * 0.5)) ? 1 : 0;
    }

    public static BufferedImage embedMessageDwt(BufferedImage image, String message) throws Exception {

        int width = image.getWidth();
        int height = image.getHeight();

        int w = width - (width % 2);
        int h = height - (height % 2);

        double[][] Y = ColorSpaceUtils.getYChannel(image);
        double[][] coeffs = new double[h][w];
        for (int y = 0; y < h; y++) {
            System.arraycopy(Y[y], 0, coeffs[y], 0, w);
        }

        DwtProcessor.forwardHaar2D(coeffs);

        byte[] payload = StegoPayload.buildPayload(message);
        BitInputStream bits = new BitInputStream(payload);

        int halfH = h / 2;
        int halfW = w / 2;

        int capacityBits = 2 * halfH * halfW;
        int neededBits = payload.length * 8;
        if (neededBits > capacityBits) {
            throw new IllegalArgumentException("Mesaj çok uzun. bit=" + neededBits + " kapasite=" + capacityBits);
        }

        outer:
        for (int y = 0; y < halfH; y++) {
            for (int x = halfW; x < w; x++) {
                if (!bits.hasNextBit()) break outer;

                double c = coeffs[y][x];
                if (Math.abs(c) < 2.0) continue;

                int bit = bits.nextBit();
                coeffs[y][x] = qimEmbed(c, bit);
            }
        }

        outer2:
        for (int y = halfH; y < h; y++) {
            for (int x = 0; x < halfW; x++) {
                if (!bits.hasNextBit()) break outer2;

                double c = coeffs[y][x];
                if (Math.abs(c) < 2.0) continue;

                int bit = bits.nextBit();
                coeffs[y][x] = qimEmbed(c, bit);
            }
        }

        if (bits.hasNextBit()) {
            throw new RuntimeException("Embed tamamlanamadı: küçük coef skip yüzünden kapasite yetmedi.");
        }

        DwtProcessor.inverseHaar2D(coeffs);

        return ColorSpaceUtils.replaceYChannel(image, coeffs);
    }

    public static String extractMessageDwt(BufferedImage stegoImage) throws Exception {

        int width = stegoImage.getWidth();
        int height = stegoImage.getHeight();

        int w = width - (width % 2);
        int h = height - (height % 2);

        double[][] Y = ColorSpaceUtils.getYChannel(stegoImage);
        double[][] coeffs = new double[h][w];
        for (int y = 0; y < h; y++) {
            System.arraycopy(Y[y], 0, coeffs[y], 0, w);
        }

        DwtProcessor.forwardHaar2D(coeffs);

        int halfH = h / 2;
        int halfW = w / 2;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int cur = 0, count = 0;

        // LH
        for (int y = 0; y < halfH; y++) {
            for (int x = halfW; x < w; x++) {
                double c = coeffs[y][x];
                if (Math.abs(c) < 2.0) continue;

                int bit = qimExtract(c);

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

        for (int y = halfH; y < h; y++) {
            for (int x = 0; x < halfW; x++) {
                double c = coeffs[y][x];
                if (Math.abs(c) < 2.0) continue;

                int bit = qimExtract(c);

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

        throw new RuntimeException("DWT stego mesajı bulunamadı (QIM).");
    }
}
