package co.poynt.samples.dcatestapp.utils;

public class Utils {

    public static byte[] invertBytes(byte[] data, int offset, int length) {
        for (int i = offset; i < length; i++) {
            data[i] ^= 0xFF;
        }
        return data;
    }
}
