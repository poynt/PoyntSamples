package co.poynt.samples.codesamples;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;


import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.poynt.os.model.AccessoryProvider;
import co.poynt.os.model.AccessoryProviderFilter;
import co.poynt.os.model.AccessoryType;
import co.poynt.os.model.PoyntError;
import co.poynt.os.model.PrinterStatus;
import co.poynt.os.services.v1.IPoyntAccessoryManagerListener;
import co.poynt.os.services.v1.IPoyntPrinterService;
import co.poynt.os.util.AccessoryProviderServiceHelper;

import static co.poynt.os.services.v1.IPoyntPrinterServiceListener.*;

public class PrinterServiceActivity extends Activity {

    public static String TAG = PrinterServiceActivity.class.getSimpleName();

    //for printer discovery
    private AccessoryProviderServiceHelper mAccessoryProviderServiceHelper;
    private HashMap<AccessoryProvider, IBinder> mPrinterServices = new HashMap<>();
    private List<AccessoryProvider> providers;
    private Set<String> printers;

    @Bind(R.id.printJobBtn)
    Button printJobBtn;
    @Bind(R.id.printReceiptBtn)
    Button printReceiptBtn;
    @Bind(R.id.printReceiptJobBtn)
    Button printReceiptJobBtn;
    @Bind(R.id.printerStatus)
    TextView printerStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer_service);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.printJobBtn)
    public void printJob(View view){
        for (IBinder binder: mPrinterServices.values()){
            final IPoyntPrinterService printerService = IPoyntPrinterService.Stub.asInterface(binder);
            if (printerService!=null){
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo);
                try {
                    printerService.printJob(UUID.randomUUID().toString(), bitmap,
                            new Stub() {
                        @Override
                        public void onPrintResponse(final PrinterStatus printerStatus, final String s) throws RemoteException {
                            Log.d(TAG, "printJob print_status  " + printerStatus.getMessage());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    printerStatusText.append("PrintJob Status: "+ printerStatus.getMessage() +  "\n");
                                }
                            });
                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // Works only on internal poynt printer
    @OnClick(R.id.printReceiptBtn)
    public void printReceiptBtn(View view){
        for (AccessoryProvider printer: mPrinterServices.keySet()){
                if(printer.getProviderName().equals("Poynt Printer")){
                    IPoyntPrinterService printerService = IPoyntPrinterService.Stub.asInterface(mPrinterServices.get(printer));
                    if (printerService!=null){
                        try {
                            printerService.printReceipt(UUID.randomUUID().toString(), PrinterServiceHelper.generateReceiptv2(), new Stub() {
                                @Override
                                public void onPrintResponse(final PrinterStatus printerStatus, String s) throws RemoteException {
                                    Log.d(TAG, "printReceipt print_status  " + printerStatus.getMessage());
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            printerStatusText.append("printReceipt Status: "+ printerStatus.getMessage() + "\n");
                                        }
                                    });
                                }
                            }, null);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
        }
    }

    @OnClick(R.id.printReceiptJobBtn)
    public void printReceiptJobBtn(View view){
        for (IBinder binder: mPrinterServices.values()){
            IPoyntPrinterService printerService = IPoyntPrinterService.Stub.asInterface(binder);
            if (printerService!=null){
                try {
                    printerService.printReceiptJob(UUID.randomUUID().toString(), PrinterServiceHelper.generateReceiptv1(),
                            new Stub() {
                                @Override
                                public void onPrintResponse(final PrinterStatus printerStatus, String s) throws RemoteException {
                                    Log.d(TAG, "printReceiptJob print_status  " + printerStatus.getMessage());
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            printerStatusText.append("printReceiptJob Status: "+ printerStatus.getMessage() + "\n");
                                        }
                                    });
                                }
                            });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
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
                providers = printers;
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
    public void discoverAndBindPrinters() {
        try {
            mAccessoryProviderServiceHelper = new AccessoryProviderServiceHelper(PrinterServiceActivity.this);
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

    public void onResume() {
        super.onResume();
        discoverAndBindPrinters();
    }

    public void onPause(){
        super.onPause();
    }
}
