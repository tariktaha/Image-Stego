package org.example.stego.dct;

import java.awt.image.BufferedImage;

public class ColorSpaceUtils {

    // RGB -> YCbCr dönüşümü (sadece Y kullanıyoruz)
    public static double[] rgbToYCbCr(int r, int g, int b) {
        double Y  =  0.299 * r + 0.587 * g + 0.114 * b;
        double Cb = -0.1687 * r - 0.3313 * g + 0.5 * b + 128;
        double Cr =  0.5 * r - 0.4187 * g - 0.0813 * b + 128;
        return new double[]{Y, Cb, Cr};
    }

    // Y kanalını çıkar
    public static double[][] getYChannel(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        double[][] Y = new double[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 255;
                int g = (rgb >> 8) & 255;
                int b = rgb & 255;

                Y[y][x] = rgbToYCbCr(r, g, b)[0];
            }
        }
        return Y;
    }

    public static BufferedImage replaceYChannel(BufferedImage original, double[][] newY) {
        int wImg = original.getWidth();
        int hImg = original.getHeight();

        int h = (newY == null) ? 0 : newY.length;
        int w = (newY == null || h == 0) ? 0 : newY[0].length;

        BufferedImage out = new BufferedImage(wImg, hImg, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < hImg; y++) {
            for (int x = 0; x < wImg; x++) {

                double Yval;

                if (y < h && x < w) {
                    Yval = newY[y][x];
                } else {
                    int rgb0 = original.getRGB(x, y);
                    int r0 = (rgb0 >> 16) & 255;
                    int g0 = (rgb0 >> 8) & 255;
                    int b0 = rgb0 & 255;
                    Yval = rgbToYCbCr(r0, g0, b0)[0];
                }

                int yy = clampTo8Bit(Yval);
                int rgb = (yy << 16) | (yy << 8) | yy; // GRAYSCALE
                out.setRGB(x, y, rgb);
            }
        }
        return out;
    }

    private static int clampTo8Bit(double v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return (int) Math.round(v);
    }
}
