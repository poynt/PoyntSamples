package co.poynt.samples.dcatestapp.utils;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

public class Utils {

    public static byte[] invertBytes(byte[] data, int offset, int length) {
        for (int i = offset; i < length; i++) {
            data[i] ^= 0xFF;
        }
        return data;
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
