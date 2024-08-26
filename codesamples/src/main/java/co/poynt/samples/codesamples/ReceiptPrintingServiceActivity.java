package co.poynt.samples.codesamples;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.UUID;

import co.poynt.os.model.Intents;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrinterStatus;
import co.poynt.os.services.v1.IPoyntReceiptPrintingService;
import co.poynt.os.services.v1.IPoyntReceiptPrintingServiceListener;
import co.poynt.os.services.v1.IPoyntReceiptSendListener;
import co.poynt.os.util.StringUtil;
import co.poynt.samples.codesamples.utils.Util;

public class ReceiptPrintingServiceActivity extends Activity {
    private final static String TAG = "ReceiptPrintingActivity";

    private IPoyntReceiptPrintingService receiptPrintingService;
    private IPoyntReceiptPrintingServiceListener receiptPrintingServiceListener = new IPoyntReceiptPrintingServiceListener.Stub() {
        @Override
        public void printQueued() throws RemoteException {
            Log.d(TAG, "Receipt queued");
        }

        @Override
        public void printFailed(PrinterStatus status) throws RemoteException {
            Log.d(TAG, "Receipt printing failed");
        }
    };

    private IPoyntReceiptSendListener receiptSendListener = new IPoyntReceiptSendListener.Stub(){

        @Override
        public void success() throws RemoteException {
            Log.d(TAG, "Receipt sent");
        }

        @Override
        public void failure() throws RemoteException {
            Log.d(TAG, "Receipt sending failed");
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
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        bindViews();
    }

    private void bindViews() {
        findViewById(R.id.printImageBtn).setOnClickListener(v -> printImage());
        findViewById(R.id.printReceiptBtn).setOnClickListener(v -> printReceipt());
        findViewById(R.id.printTxnReceiptBtn).setOnClickListener(v -> printTxnReceipt());
        findViewById(R.id.printOrderReceiptBtn).setOnClickListener(v -> printOrderReceipt());
        findViewById(R.id.sendReceiptBtn).setOnClickListener(v -> sendReceipt());
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

        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public void printTxnReceipt() {
        String jobId = UUID.randomUUID().toString();
        String transactionId = "d9078bbc-6db5-41e8-be34-d9fb4f147c6f";
        long tipAmount = 100;
        if (receiptPrintingService != null) {
            try {
                Toast.makeText(this, "Update the transaction Id if the receipt does not print", Toast.LENGTH_LONG).show();
                receiptPrintingService.printTransactionReceipt(jobId, transactionId, tipAmount, receiptPrintingServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void printOrderReceipt() {
        String jobId = UUID.randomUUID().toString();
        String orderId = "2182e387-016c-1000-04ee-1e22b9cf2a80";
        if (receiptPrintingService != null) {
            try {
                Toast.makeText(this, "Update the Order Id if the receipt does not print", Toast.LENGTH_LONG).show();
                receiptPrintingService.printOrderReceipt(jobId, orderId, receiptPrintingServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    // TODO Fix this and enable
//    @OnClick(R.id.printReceiptOptBtn)
//    public void printReceiptOptBtn() {
//        String jobId = UUID.randomUUID().toString();
//        Bundle options = new Bundle();
//        Order order = Util.generateOrder();
////        options.putParcelable("ORDER", order);
//        options.putParcelable("TRANSACTION", Util.generateTransaction(order.getAmounts().getNetTotal()));
//        if (receiptPrintingService != null) {
//            try {
//                receiptPrintingService.printReceiptWithOptions(jobId, options, receiptPrintingServiceListener);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public void sendReceipt() {
        String transactionId = "d9078bbc-6db5-41e8-be34-d9fb4f147c6f";
        String orderId = "2182e387-016c-1000-04ee-1e22b9cf2a80";
        String email = "";
        String phone = "";
        if (receiptPrintingService != null) {
            try {
                if (StringUtil.isEmpty(email) && StringUtil.isEmpty(phone)){
                    Toast.makeText(this, "Fill in phone or email info in code", Toast.LENGTH_LONG).show();
                }else {
                    receiptPrintingService.sendReceipt(orderId, transactionId, email, phone,
                            receiptSendListener);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void printImage() {
        String jobId = UUID.randomUUID().toString();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.receipt3);
        if (receiptPrintingService != null) {
            try {
                receiptPrintingService.printBitmap(jobId, bitmap, receiptPrintingServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void printReceipt() {
        String jobId = UUID.randomUUID().toString();
        PrintedReceipt receipt = Util.generateReceipt(getResources());
        if (receiptPrintingService != null) {
            try {
                receiptPrintingService.printReceipt(jobId, receipt, receiptPrintingServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
