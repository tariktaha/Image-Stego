package org.example.stego.dwt;

public class DwtProcessor {

    public static void forwardHaar1D(double[] data) {
        int n = data.length;
        int half = n / 2;

        double[] temp = new double[n];

        for (int i = 0; i < half; i++) {
            double a = data[2 * i];
            double b = data[2 * i + 1];

            temp[i] = (a + b) / 2.0;
            temp[half + i] = (a - b) / 2.0;
        }

        System.arraycopy(temp, 0, data, 0, n);
    }

    public static void inverseHaar1D(double[] data) {
        int n = data.length;
        int half = n / 2;

        double[] temp = new double[n];

        for (int i = 0; i < half; i++) {
            double a = data[i];
            double d = data[half + i];

            temp[2 * i] = a + d;
            temp[2 * i + 1] = a - d;
        }

        System.arraycopy(temp, 0, data, 0, n);
    }

    public static void forwardHaar2D(double[][] data) {
        int h = data.length;
        int w = data[0].length;

        double[] row = new double[w];
        for (int y = 0; y < h; y++) {
            System.arraycopy(data[y], 0, row, 0, w);
            forwardHaar1D(row);
            System.arraycopy(row, 0, data[y], 0, w);
        }

        double[] col = new double[h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                col[y] = data[y][x];
            }
            forwardHaar1D(col);
            for (int y = 0; y < h; y++) {
                data[y][x] = col[y];
            }
        }
    }

    public static void inverseHaar2D(double[][] data) {
        int h = data.length;
        int w = data[0].length;

        double[] col = new double[h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                col[y] = data[y][x];
            }
            inverseHaar1D(col);
            for (int y = 0; y < h; y++) {
                data[y][x] = col[y];
            }
        }

        double[] row = new double[w];
        for (int y = 0; y < h; y++) {
            System.arraycopy(data[y], 0, row, 0, w);
            inverseHaar1D(row);
            System.arraycopy(row, 0, data[y], 0, w);
        }
    }
}
