package org.example.stego.dct;

public class BitInputStream {
    private final byte[] data;
    private int bytePos = 0;
    private int bitPos = 0;

    public BitInputStream(byte[] data) {
        this.data = data;
    }

    public boolean hasNextBit() {
        return bytePos < data.length;
    }

    public int nextBit() {
        if (!hasNextBit()) throw new IllegalStateException("No more bits");
        int b = data[bytePos] & 0xFF;
        int bit = (b >> (7 - bitPos)) & 1;

        bitPos++;
        if (bitPos == 8) {
            bitPos = 0;
            bytePos++;
        }
        return bit;
    }
}
