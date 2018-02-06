package co.poynt.samples.codesamples;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import co.poynt.os.Constants;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.printing.ReceiptPrintingPref;
import co.poynt.os.model.PrintedReceiptSection;
import co.poynt.os.services.v1.IPoyntReceiptDecoratorService;
import co.poynt.os.services.v1.IPoyntReceiptDecoratorServiceListener;

/**
 * Created by dennis on 8/27/17.
 */

public class ReceiptCustomizationService extends Service {

    private static final String TAG = ReceiptCustomizationService.class.getSimpleName();

    private IPoyntReceiptDecoratorService.Stub mBinder = new IPoyntReceiptDecoratorService.Stub() {
        @Override
        public void decorate(PrintedReceipt printedReceipt, String requestId, IPoyntReceiptDecoratorServiceListener
                listener) throws RemoteException {
            List<PrintedReceiptLine> headerLines = printedReceipt.getHeader();

            if (headerLines == null) {
                headerLines = new ArrayList<>();
            }

            for (PrintedReceiptLine line : headerLines) {
                Log.d(TAG, "decorate: " + line.getText());
            }
            PrintedReceiptLine EMPTY_LINE = new PrintedReceiptLine();
            EMPTY_LINE.setText("\n");

            PrintedReceiptLine twitterInfo = new PrintedReceiptLine();
            twitterInfo.setText("We are on twitter @poynt");

            headerLines.add(EMPTY_LINE);
            headerLines.add(twitterInfo);
            headerLines.add(EMPTY_LINE);
            headerLines.add(EMPTY_LINE);
            headerLines.add(EMPTY_LINE);

            printedReceipt.setHeader(headerLines);
            // to avoid printing images Poynt Smart Terminal which is running on battery
            if(!isPoynt61() || isConnected() || !usesPoyntPrinter()) {
                printedReceipt.setHeaderImage(BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo));
            }else{
                Handler h = new Handler(getMainLooper());
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ReceiptCustomizationService.this, "Put the terminal on dock to print images", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            listener.onReceiptDecorated(requestId, printedReceipt);
        }

        @Override
        public void cancel(String requestId) throws RemoteException {

        }

        @Override
        public void decorateV2(co.poynt.os.model.PrintedReceiptV2 printedReceiptV2,
                               String requestId, co.poynt.os.services.v1.IPoyntReceiptDecoratorListener listener) throws RemoteException {
            PrintedReceiptSection headerSection = printedReceiptV2.getHeader();
            List<PrintedReceiptLine> headerLines = headerSection.getLines();

            if (headerLines == null) {
                headerLines = new ArrayList<>();
            }

            for (PrintedReceiptLine line : headerLines) {
                Log.d(TAG, "decorate: " + line.getText());
            }
            PrintedReceiptLine EMPTY_LINE = new PrintedReceiptLine();
            EMPTY_LINE.setText("\n");

            PrintedReceiptLine twitterInfo = new PrintedReceiptLine();
            twitterInfo.setText("We are on twitter @poynt");

            headerLines.add(EMPTY_LINE);
            headerLines.add(twitterInfo);
            headerLines.add(EMPTY_LINE);
            headerLines.add(EMPTY_LINE);
            headerLines.add(EMPTY_LINE);

            headerSection.setLines(headerLines);
            printedReceiptV2.setHeader(headerSection);
            printedReceiptV2.setHeaderImage(BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo));
            listener.onReceiptDecorated(requestId, printedReceiptV2);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean isConnected() {
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    private boolean usesPoyntPrinter() {
        boolean usesPoyntPrinter = false;
        Set<String> set = ReceiptPrintingPref.readReceiptPrefsFromDb(this, Constants.ReceiptPreference.PREF_PAYMENT_RECEIPT);
        set.addAll(ReceiptPrintingPref.readReceiptPrefsFromDb(this, Constants.ReceiptPreference.PREF_ITEM_RECEIPT));
        set.addAll(ReceiptPrintingPref.readReceiptPrefsFromDb(this, Constants.ReceiptPreference.PREF_CUSTOMER_RECEIPT));
        set.addAll(ReceiptPrintingPref.readReceiptPrefsFromDb(this, Constants.ReceiptPreference.PREF_ORDER_RECEIPT));
        if (set!=null && set.size() > 0) {
            if (set.contains(Constants.ReceiptPreference.POYNT_PRINTER)){
                Log.d(TAG, "found: " + "Poynt Printer");
                usesPoyntPrinter = true;
            }
        }
        return usesPoyntPrinter;
    }

    public static boolean isPoynt61() {
        return "Poynt-P61".equals(Build.MODEL);
    }

}