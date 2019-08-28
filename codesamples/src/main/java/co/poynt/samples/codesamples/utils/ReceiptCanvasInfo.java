package co.poynt.samples.codesamples.utils;

import co.poynt.os.model.PrintedReceiptLineFont;
import co.poynt.os.model.PrintedReceiptSection;
import co.poynt.os.model.PrintedReceiptV2;

public class ReceiptCanvasInfo {
    public static final int MILS_OF_INCH = 1000;
    public static final int MDPI = 160;
    public static final int TVDPI = 213; //1.33 * MDPI

    public static final float TVDPI_FACTOR = 1.33f;

    public static final int PRINTER_DPI_X = 203;
    public static final int PRINTER_DPI_Y = 203;

    public static final int PAPER_WIDTH = 400; //in Pixels

    private static final String BLANK_LINE = "                          ";

    private static final int FONT_SIZE_DEFAULT = 24;
    private static final int LINE_SPACING_DEFAULT = 26;
    private static final int MAX_TEXT_LINE_LENGTH_DEFAULT = 26;
    private static final int LEFT_MARGIN = 0;//8;

    private static final int MAX_IMAGE_WIDTH_PX = 240;
    private static final int MAX_IMAGE_HEIGHT_PX = 200;
    public static final int PAPER_TEAR_SPACE = LINE_SPACING_DEFAULT * 4;

    private static final int MAX_TEXT_LINE_LENGTH_FONT_0 = 48;
    private static final int MAX_TEXT_LINE_LENGTH_FONT_1 = 32;
    private static final int MAX_TEXT_LINE_LENGTH_FONT_2 = 24;
    private static final int MAX_TEXT_LINE_LENGTH_FONT_3 = 32;

    public enum PrintType {
        TEXT,
        IMAGE
    }

    public static int calculateTextHeight(int lines) {
        return lines * LINE_SPACING_DEFAULT;
    }

    public static int calculateTextHeight(PrintedReceiptV2 receipt) {
        int result = 0;

        if (receipt == null) {
            return 0;
        }

        //Calculate Height for each section individually
        result += getHeightForSection(receipt.getHeader());
        result += getHeightForSection(receipt.getMerchantInfo());
        result += getHeightForSection(receipt.getBody1());
        result += getHeightForSection(receipt.getBody2());
        result += getHeightForSection(receipt.getBody3());
        result += getHeightForSection(receipt.getBody4());
        result += getHeightForSection(receipt.getBody5());
        result += getHeightForSection(receipt.getBody6());
        result += getHeightForSection(receipt.getFooter());

        return result;
    }

    public static int getHeightForSection(PrintedReceiptSection section) {
        int result = 0;
        if (section != null && section.getLines() != null) {
            result = (section.getLines().size()) * (section.getFont() != null ? section.getFont().getFontSpacing() : LINE_SPACING_DEFAULT);
        }

        return result;
    }

    public static int calculateImageHeight(int heightPx) {
        return (int) (heightPx * TVDPI_FACTOR);
    }

    public static int getLeftMargin() {
        return LEFT_MARGIN;
    }

    public static int getMaxImageWidth() {
        return MAX_IMAGE_WIDTH_PX;
    }

    public static int getMaxImageHeight() {
        return MAX_IMAGE_HEIGHT_PX;
    }

    public static String getBlankLine() {
        return BLANK_LINE;
    }

    public static String getBlankLine(PrintedReceiptLineFont font) {
        if (font != null && font.getFontSize() != null) {
            switch (font.getFontSize()) {
                case FONT15:
                    return String.format("%1$42s", " ");
                case FONT17:
                    return String.format("%1$32s", " ");
                case FONT24:
                    return String.format("%1$26s", " ");
                case FONT30:
                    return String.format("%1$14s", " ");
            }
        }
        return BLANK_LINE;
    }

    public static int getMaxTextLineLength() {
        return MAX_TEXT_LINE_LENGTH_DEFAULT;
    }

    public static int getDefaultFontSize() {
        return FONT_SIZE_DEFAULT;
    }

    public static int getMaxTextLineLength(PrintType type, PrintedReceiptLineFont font) {

        if (type == PrintType.IMAGE) {
            if (font != null && font.getFontSize() != null) {
                switch (font.getFontSize()) {

                    case FONT15:
                        return 40;
                    case FONT17:
                        return 32;
                    case FONT24:
                        return 26;
                    case FONT30:
                        return 14;
                }
            }
            return MAX_TEXT_LINE_LENGTH_DEFAULT;
        } else {
            // Text Type
            if (font != null) {
                switch (font.getFontSize()) {
                    case FONT15:
                        return MAX_TEXT_LINE_LENGTH_FONT_0;
                    case FONT17:
                        return MAX_TEXT_LINE_LENGTH_FONT_3;
                    case FONT24:
                        return MAX_TEXT_LINE_LENGTH_FONT_1;
                    case FONT30:
                        return MAX_TEXT_LINE_LENGTH_FONT_2;
                    default:
                        return MAX_TEXT_LINE_LENGTH_FONT_1;

                }
            }
            return MAX_TEXT_LINE_LENGTH_FONT_1;
        }
    }

    public static int getLineSpacing(PrintedReceiptLineFont font) {
        if (font != null) {
            return font.getFontSpacing();
        }
        return LINE_SPACING_DEFAULT;
    }

    public static int getDefaultLineSpacing() {
        return LINE_SPACING_DEFAULT;
    }
}
