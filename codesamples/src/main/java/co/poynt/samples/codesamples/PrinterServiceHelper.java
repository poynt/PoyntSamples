package co.poynt.samples.codesamples;

import java.util.ArrayList;
import java.util.List;

import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.model.PrintedReceiptLineFont;
import co.poynt.os.model.PrintedReceiptLineV2;
import co.poynt.os.model.PrintedReceiptSection;
import co.poynt.os.model.PrintedReceiptV2;

public class PrinterServiceHelper {

    public static PrintedReceipt generateReceiptv1(){
        PrintedReceipt printedReceipt = new PrintedReceipt();

        // BODY
        List<PrintedReceiptLine> body = new ArrayList<PrintedReceiptLine>();

        body.add(newLine(" Check-in REWARD  "));
        body.add(newLine(""));
        body.add(newLine("FREE Reg. 1/2 Order"));
        body.add(newLine("Nachos or CHEESE"));
        body.add(newLine("Quesadilla with min."));
        body.add(newLine("$ 15 bill."));
        body.add(newLine(".................."));
        body.add(newLine("John Doe"));
        body.add(newLine("BD: May-5, AN: Aug-4"));
        body.add(newLine("john.doe@gmail.com"));
        body.add(newLine("Visit #23"));
        body.add(newLine("Member since: 15 June 2013"));
        body.add(newLine(".................."));
        body.add(newLine("Apr-5-2013 3:25 PM"));
        body.add(newLine("Casa Orozco, Dublin, CA"));
        body.add(newLine(".................."));
        body.add(newLine("John Doe"));
        body.add(newLine("BD: May-5, AN: Aug-4"));
        body.add(newLine("john.doe@gmail.com"));
        body.add(newLine("Visit #23"));
        body.add(newLine("Member since: 15 June 2013"));
        body.add(newLine(".................."));
        body.add(newLine("Apr-5-2013 3:25 PM"));
        body.add(newLine("Casa Orozco, Dublin, CA"));
        body.add(newLine(".................."));
        body.add(newLine("Coupon#: 1234-5678"));
        body.add(newLine("  Powered by Poynt"));
        printedReceipt.setBody(body);

        // to print image
//        printedReceipt.setHeaderImage(BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo));
//        printedReceipt.setFooterImage(BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo));

        return printedReceipt;
    }

    public static PrintedReceiptLine newLine(String s){
        PrintedReceiptLine line = new PrintedReceiptLine();
        line.setText(s);
        return line;
    }


    public static PrintedReceiptV2 generateReceiptv2(){
        PrintedReceiptV2 printedReceipt = new PrintedReceiptV2();

        // Section
        List<PrintedReceiptLine> body = new ArrayList<PrintedReceiptLine>();

        body.add(newLine(" Check-in REWARD  "));
        body.add(newLine(""));
        body.add(newLine("FREE Reg. 1/2 Order"));
        body.add(newLine("Nachos or CHEESE"));
        body.add(newLine("Quesadilla with min."));
        body.add(newLine("$ 15 bill."));
        body.add(newLine(".................."));
        body.add(newLine("John Doe"));
        body.add(newLine("BD: May-5, AN: Aug-4"));
        body.add(newLine("john.doe@gmail.com"));
        body.add(newLine("Visit #23"));
        body.add(newLine("Member since: 15 June 2013"));
        body.add(newLine(".................."));
        body.add(newLine("Apr-5-2013 3:25 PM"));
        body.add(newLine("Casa Orozco, Dublin, CA"));
        body.add(newLine(".................."));
        body.add(newLine("John Doe"));
        body.add(newLine("BD: May-5, AN: Aug-4"));
        body.add(newLine("john.doe@gmail.com"));
        body.add(newLine("Visit #23"));
        body.add(newLine("Member since: 15 June 2013"));
        body.add(newLine(".................."));
        body.add(newLine("Apr-5-2013 3:25 PM"));
        body.add(newLine("Casa Orozco, Dublin, CA"));
        body.add(newLine(".................."));
        body.add(newLine("Coupon#: 1234-5678"));
        body.add(newLine("  Powered by Poynt"));

        // Set Font
        PrintedReceiptLineFont font = new PrintedReceiptLineFont(PrintedReceiptLineFont.FONT_SIZE.FONT17, 22);
        PrintedReceiptSection bodySection = new PrintedReceiptSection(body, font);

        printedReceipt.setBody1(bodySection);
        // to print image
//        printedReceipt.setHeaderImage(BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo));
//        printedReceipt.setFooterImage(BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo));

        return printedReceipt;
    }

}
