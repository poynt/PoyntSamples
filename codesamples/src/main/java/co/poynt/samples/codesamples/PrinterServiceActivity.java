package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.poynt.os.model.AccessoryProvider;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.model.PrintedReceiptLineFont;
import co.poynt.os.model.PrintedReceiptSection;
import co.poynt.os.model.PrintedReceiptV2;
import co.poynt.os.model.PrinterStatus;
import co.poynt.os.services.v1.IPoyntPrinterService;
import co.poynt.samples.codesamples.utils.PrinterServiceHelper;

public class PrinterServiceActivity extends Activity{

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
        printerServiceHelper = new PrinterServiceHelper(this, printerCallback);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_refresh:
                printerServiceHelper.refreshPrinters();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_printer_service, menu);
        return true;
    }

    @OnClick(R.id.printJobBtn)
    public void printJob(View view) {
        HashMap<AccessoryProvider, IBinder> printers = printerServiceHelper.getPrinters();
        for (AccessoryProvider printer: printers.keySet()) {
            if (printer.isConnected()) {
                final IPoyntPrinterService printerService = IPoyntPrinterService.Stub.asInterface(printers.get(printer));
                if (printerService != null) {
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo);
                    try {
                        printerService.printJob(UUID.randomUUID().toString(), bitmap, printerServiceHelper.printerServiceListener);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                printerStatusText.append(printer.getProviderName() + ": " + String.valueOf(printer.isConnected()));
                printerServiceHelper.reconnectPrinter(printer);
            }
        }
    }

    // Works only on internal poynt printer
    @OnClick(R.id.printReceiptBtn)
    public void printReceiptBtn(View view) {
        mPrinterServices = printerServiceHelper.getPrinters();
        for (final AccessoryProvider printer : mPrinterServices.keySet()) {
            Log.d(TAG, printer.getProviderName());
            if (printer.getProviderName().equals("Poynt Printer")) {
                IPoyntPrinterService printerService = IPoyntPrinterService.Stub.asInterface(mPrinterServices.get(printer));
                if (printerService != null) {
                    try {
                        printerService.printReceipt(UUID.randomUUID().toString(),
                                generateReceiptv2(),printerServiceHelper.printerServiceListener, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @OnClick(R.id.printReceiptJobBtn)
    public void printReceiptJobBtn(View view) {
        for (IBinder binder : printerServiceHelper.getPrinters().values()) {
            IPoyntPrinterService printerService = IPoyntPrinterService.Stub.asInterface(binder);
            if (printerService != null) {
                try {
                    printerService.printReceiptJob(UUID.randomUUID().toString(),
                            generateReceiptv1(),printerServiceHelper.printerServiceListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    PrinterServiceHelper.PrinterCallback printerCallback = new PrinterServiceHelper.PrinterCallback() {
        @Override
        public void onPrinterResponse(final PrinterStatus status) {
            if(status.getCode().equals(PrinterStatus.Code.PRINTER_DISCONNECTED)){
                printerStatusText.append("Printer disconnected");
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printerStatusText.append(status.getMessage()+"\n");
                }
            });
        }

        @Override
        public void onPrinterReconnect(IBinder binder) {
        }

        @Override
        public void logMessage(final String s) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printerStatusText.append(s +"\n");
                }
            });
        }
    };

    public static PrintedReceipt generateReceiptv1() {
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

    public static PrintedReceiptLine newLine(String s) {
        PrintedReceiptLine line = new PrintedReceiptLine();
        line.setText(s);
        return line;
    }


    public static PrintedReceiptV2 generateReceiptv2() {
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
    public void onResume() {
        super.onResume();
        //Bind to accessory manager which then binds to all printers
        printerServiceHelper.bindAccessoryManager();

    }

    public void onPause() {
        super.onPause();
        printerServiceHelper.unBindServices();
    }


}
