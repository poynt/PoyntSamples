package co.poynt.samples.dcatestapp.utils;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import co.poynt.os.util.ByteUtils;

public class Utils {

    public static byte[] invertBytes(byte[] data, int offset, int length) {
        for (int i = offset; i < length; i++) {
            data[i] ^= 0xFF;
        }
        return data;
    }


    public static String decryptData(String data, String defaultKey) throws Exception {
        // Convert the defaultKey and response to byte arrays
        byte[] keyBytes = ByteUtils.hexStringToByteArray(defaultKey);
        byte[] dataBytes = ByteUtils.hexStringToByteArray(data);

        // Initialize the AES cipher in CBC mode without padding
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

        // Create an IV (initialization vector) - for simplicity, using a zero IV here
        byte[] iv = new byte[16]; // 16 bytes for AES
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Initialize the cipher for decryption
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        // Decrypt the response
        byte[] decryptedBytes = cipher.doFinal(dataBytes);

        // Convert decrypted bytes to string
        return ByteUtils.byteArrayToHexString(decryptedBytes);
    }

    public static String encryptData(String data, String defaultKey) throws Exception {
        // Convert the defaultKey and data to byte arrays
        byte[] keyBytes = ByteUtils.hexStringToByteArray(defaultKey);
        byte[] dataBytes = ByteUtils.hexStringToByteArray(data);

        // Initialize the AES cipher in CBC mode without padding
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

        // Create an IV (initialization vector) - for simplicity, using a zero IV here
        byte[] iv = new byte[16]; // 16 bytes for AES
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Initialize the cipher for encryption
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        // Encrypt the data
        byte[] encryptedBytes = cipher.doFinal(dataBytes);

        // Convert encrypted bytes to hex string
        return ByteUtils.byteArrayToHexString(encryptedBytes);
    }

    public static String rotateStringByOneByte(String input) {
        byte[] byteArray = ByteUtils.hexStringToByteArray(input);
        if (byteArray.length > 1) {
            byte firstByte = byteArray[0];
            System.arraycopy(byteArray, 1, byteArray, 0, byteArray.length - 1);
            byteArray[byteArray.length - 1] = firstByte;
        }
        return ByteUtils.byteArrayToHexString(byteArray);
    }

    public static SpannableString getColoredString(String text) {
        SpannableString spannableString = new SpannableString(text);
        text = text.toLowerCase();
        String passed = "passed";
        String failed = "failed";
        String separator = "=======";
        boolean containsPassed = text.contains(passed);
        boolean containsFailed = text.contains(failed);
        boolean containsSeparator = text.contains(separator);
        if (containsPassed || containsFailed) {
            int color = containsPassed
                    ? Color.parseColor("#FF01741A")
                    : Color.parseColor("#FF740112");
            spannableString.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if(containsSeparator) {
            spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }
}
