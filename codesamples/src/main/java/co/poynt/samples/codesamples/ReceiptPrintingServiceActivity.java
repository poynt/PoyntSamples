package co.poynt.samples.codesamples;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.model.PrinterStatus;
import co.poynt.os.services.v1.IPoyntReceiptPrintingService;
import co.poynt.os.services.v1.IPoyntReceiptPrintingServiceListener;

public class ReceiptPrintingServiceActivity extends Activity {
    private final static String TAG = "ReceiptPrintingActivity";
    @Bind(R.id.printImageBtn) Button printImageBtn;
    @Bind(R.id.printReceiptBtn) Button printReceiptBtn;


    private IPoyntReceiptPrintingService receiptPrintingService;
    private IPoyntReceiptPrintingServiceListener receiptPrintingServiceListener = new IPoyntReceiptPrintingServiceListener.Stub(){
        @Override
        public void printQueued() throws RemoteException {
            Log.d(TAG, "Receipt queued");
        }
        @Override
        public void printFailed(PrinterStatus status) throws RemoteException {
            Log.d(TAG, "Receipt printing failed");
        }
    };
    private ServiceConnection receiptServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            receiptPrintingService = IPoyntReceiptPrintingService.Stub.asInterface(iBinder);
            Log.d(TAG, "Receiptprintingservice connection established");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            receiptPrintingService = null;
            Log.d(TAG, "Receiptprintingservice connection disconnected");
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_printing_service);
        android.app.ActionBar actionBar = getActionBar();
        if (actionBar !=null ) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ButterKnife.bind(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_RECEIPT_PRINTING_SERVICE),
                receiptServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(receiptServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_receipt_printing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id==android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.printImageBtn)
    public void printImage(){
        String jobId = UUID.randomUUID().toString();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.receipt3);
        if (receiptPrintingService != null){
            try {
                receiptPrintingService.printBitmap(jobId, bitmap, receiptPrintingServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @OnClick(R.id.printReceiptBtn)
    public void printReceipt(){
        String jobId = UUID.randomUUID().toString();
        PrintedReceipt receipt = generateReceipt();
        if (receiptPrintingService != null){
            try {
                receiptPrintingService.printReceipt(jobId, receipt, receiptPrintingServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private PrintedReceipt generateReceipt(){
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
        body.add(newLine("Coupon#: 1234-5678"));
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
        body.add(newLine("Coupon#: 1234-5678"));
        body.add(newLine("  Powered by Poynt"));
        printedReceipt.setBody(body);

        // to print image
        printedReceipt.setHeaderImage(BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo));
        printedReceipt.setFooterImage(BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo));

        return printedReceipt;
    }

    private PrintedReceiptLine newLine(String s){
        PrintedReceiptLine line = new PrintedReceiptLine();
        line.setText(s);
        return line;
    }
}
