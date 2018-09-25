package co.poynt.samples.codesamples;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import co.poynt.os.model.AccessoryProvider;
import co.poynt.os.model.AccessoryProviderFilter;
import co.poynt.os.model.AccessoryType;
import co.poynt.os.model.PoyntError;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.model.PrintedReceiptLineFont;
import co.poynt.os.model.PrintedReceiptSection;
import co.poynt.os.model.PrintedReceiptV2;
import co.poynt.os.services.v1.IPoyntAccessoryManagerListener;
import co.poynt.os.util.AccessoryProviderServiceHelper;

public class PrinterServiceHelper {
    private static final String TAG = PrinterServiceHelper.class.getSimpleName();

    //for printer discovery
    private AccessoryProviderServiceHelper mAccessoryProviderServiceHelper;
    private HashMap<AccessoryProvider, IBinder> mPrinterServices = new HashMap<>();

    public  PrinterServiceHelper (Context context){
        discoverAndBindPrinters(context);
    }

    public HashMap<AccessoryProvider, IBinder> getPrinters(){
        return mPrinterServices;
    }

    private AccessoryProviderServiceHelper.ProviderConnectionCallback providerConnectionCallback
            = new AccessoryProviderServiceHelper.ProviderConnectionCallback() {
        @Override
        public void onConnected(AccessoryProvider provider, IBinder binder) {
            mPrinterServices.put(provider, binder);
        }

        @Override
        public void onDisconnected(AccessoryProvider provider, IBinder binder) {
            if (mPrinterServices != null && mPrinterServices.size() > 0) {
                mPrinterServices.remove(binder);
                if (mAccessoryProviderServiceHelper.getAccessoryServiceManager() != null) {
                    IBinder binder2 = mAccessoryProviderServiceHelper.getAccessoryService(
                            provider, AccessoryType.PRINTER,
                            providerConnectionCallback);
                    if (binder2 != null) {
                        mPrinterServices.put(provider, binder2);
                    }
                }
            }
        }
    };

    /**
     * this is the accessory manager listener which gets invoked when accessory manager completes
     * scanning for the requested accessories
     */
    private IPoyntAccessoryManagerListener poyntAccessoryManagerListener
            = new IPoyntAccessoryManagerListener.Stub() {

        @Override
        public void onError(PoyntError poyntError) {
            Log.e(TAG, "Failed to connect to accessory manager: " + poyntError);
        }

        @Override
        public void onSuccess(final List<AccessoryProvider> printers) {
            if (printers != null && printers.size() > 0) {
                if (mAccessoryProviderServiceHelper.getAccessoryServiceManager() != null) {
                    for (AccessoryProvider printer : printers) {
                        Log.d(TAG, "Printer: " + printer.toString());
                        if (printer.isConnected()) {
                            IBinder binder = mAccessoryProviderServiceHelper.getAccessoryService(
                                    printer, AccessoryType.PRINTER,
                                    providerConnectionCallback);
                            if (binder != null) {
                                mPrinterServices.put(printer, binder);
                            }
                        }
                    }
                }
            } else {
                Log.d(TAG, "No Printers found");
            }
        }
    };


    /**
     * Bind to printers
     */
    public void discoverAndBindPrinters(Context context) {
        try {
            mAccessoryProviderServiceHelper = new AccessoryProviderServiceHelper(context);
            mAccessoryProviderServiceHelper.bindAccessoryManager(
                    new AccessoryProviderServiceHelper.AccessoryManagerConnectionCallback() {
                        @Override
                        public void onConnected(AccessoryProviderServiceHelper accessoryProvider) {
                            // Check for connected accessories and filter printers
                            if (accessoryProvider.getAccessoryServiceManager() != null) {
                                AccessoryProviderFilter filter = new AccessoryProviderFilter(AccessoryType.PRINTER);
                                try {
                                    Log.d(TAG, "Connected to Accessory provider service");
                                    accessoryProvider.getAccessoryServiceManager().getAccessoryProviders(
                                            filter, poyntAccessoryManagerListener);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Unable to connect to Accessory Service", e);
                                }
                            } else {
                                Log.e(TAG, "Unable to fetch accessory service manager");
                            }
                        }

                        @Override
                        public void onDisconnected(AccessoryProviderServiceHelper accessoryProviderServiceHelper) {
                            Log.e(TAG, "Disconnected from accessory service manager");
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to connect to capability or accessory manager", e);
        }
    }
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
