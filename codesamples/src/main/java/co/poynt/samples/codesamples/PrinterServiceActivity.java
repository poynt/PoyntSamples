package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.UUID;


import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.poynt.os.model.AccessoryProvider;
import co.poynt.os.model.PrinterStatus;
import co.poynt.os.services.v1.IPoyntPrinterService;

import static co.poynt.os.services.v1.IPoyntPrinterServiceListener.*;

public class PrinterServiceActivity extends Activity {

    public static String TAG = PrinterServiceActivity.class.getSimpleName();
    private PrinterServiceHelper printerServiceHelper;
    private HashMap<AccessoryProvider, IBinder> mPrinterServices = new HashMap<>();


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
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        ButterKnife.bind(this);

        printerServiceHelper = new PrinterServiceHelper(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.printJobBtn)
    public void printJob(View view){
        for (IBinder binder: printerServiceHelper.getPrinters().values()){
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
        mPrinterServices = printerServiceHelper.getPrinters();
        for (AccessoryProvider printer: mPrinterServices.keySet()){
            Log.d(TAG, printer.getProviderName());
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
        for (IBinder binder: printerServiceHelper.getPrinters().values()){
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


    public void onResume() {
        super.onResume();
    }

    public void onPause(){
        super.onPause();
    }
}
