package org.example.stego.dct;

public class DctProcessor {

    private static final int N = 8;
    private static final double SQRT_1_2 = 1.0 / Math.sqrt(2.0);

    public static double[][] extract8x8(double[][] src, int sy, int sx) {
        double[][] block = new double[N][N];
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++)
                if (sy + i < src.length && sx + j < src[0].length)
                    block[i][j] = src[sy + i][sx + j];
        return block;
    }

    public static void forwardDCT(double[][] block) {
        double[][] tmp = new double[N][N];

        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {

                double sum = 0.0;

                for (int x = 0; x < N; x++) {
                    for (int y = 0; y < N; y++) {
                        sum += block[x][y] *
                                Math.cos(((2 * x + 1) * u * Math.PI) / (2 * N)) *
                                Math.cos(((2 * y + 1) * v * Math.PI) / (2 * N));
                    }
                }

                double cu = (u == 0) ? SQRT_1_2 : 1.0;
                double cv = (v == 0) ? SQRT_1_2 : 1.0;

                tmp[u][v] = 0.25 * cu * cv * sum;
            }
        }

        for (int i = 0; i < N; i++)
            System.arraycopy(tmp[i], 0, block[i], 0, N);
    }

    public static void inverseDCT(double[][] block) {
        double[][] tmp = new double[N][N];

        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {

                double sum = 0.0;

                for (int u = 0; u < N; u++) {
                    for (int v = 0; v < N; v++) {

                        double cu = (u == 0) ? SQRT_1_2 : 1.0;
                        double cv = (v == 0) ? SQRT_1_2 : 1.0;

                        sum += cu * cv * block[u][v] *
                                Math.cos(((2 * x + 1) * u * Math.PI) / (2 * N)) *
                                Math.cos(((2 * y + 1) * v * Math.PI) / (2 * N));
                    }
                }

                tmp[x][y] = 0.25 * sum;
            }
        }

        for (int i = 0; i < N; i++)
            System.arraycopy(tmp[i], 0, block[i], 0, N);
    }
}
