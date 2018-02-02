package co.poynt.samples.codesamples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import co.poynt.os.model.Intents;

public class MainActivity extends Activity {
    private Button transactionListBtn;
    private Button terminalUserLoginBtn;
    private Button orderBtn;
    private Button tokenServiceBtn;
    private Button paymentFragmentBtn;
    private Button scannerActivityBtn;
    private Button secondScreenServiceActivityBtn, secondScreenServiceV2ActivityBtn;
    private Button receiptPrintingServiceActivityBtn;
    private Button productServiceActivityBtn;
    private Button businessServiceActivityBtn;
    private Button billingServiceActivityBtn;
    private Button accessoriesActivityBtn;
    private Button cameraActivityBtn;
    private Button nonPaymentCardReaderActivityBtn;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // triggers transaction sync; requires permission poynt.permission.LAST_SYNC_TIME_INFO in manifest
        Intent sendingIntent = new Intent(Intents.ACTION_SYNC_TRANSACTIONS_FROM_CLOUD);
        sendBroadcast(sendingIntent);

        setContentView(R.layout.activity_main);

//        Button contentProviderBtn = (Button) findViewById(R.id.contentProviderBtn);
//        contentProviderBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, ContentProviderSampleActivity.class);
//                startActivity(intent);
//            }
//        });
        transactionListBtn = (Button) findViewById(R.id.transactionListBtn);
        transactionListBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TransactionListActivity.class);
                startActivity(intent);
            }
        });

        terminalUserLoginBtn = (Button) findViewById(R.id.terminalUserLoginBtn);
        terminalUserLoginBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        orderBtn = (Button) findViewById(R.id.orderBtn);
        orderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OrderActivity.class);
                startActivity(intent);
            }
        });

        tokenServiceBtn = (Button) findViewById(R.id.tokenServiceBtn);
        tokenServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, TokenServiceActivity.class);
                startActivity(intent);
            }
        });

        paymentFragmentBtn = (Button) findViewById(R.id.paymentFragmentBtn);
        paymentFragmentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PaymentActivity.class);
                startActivity(intent);
            }
        });

        scannerActivityBtn = (Button) findViewById(R.id.scannerActivityBtn);
        scannerActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ScannerActivity.class);
                startActivity(intent);
            }
        });

        secondScreenServiceActivityBtn = (Button) findViewById(R.id.secondScreenServiceActivityBtn);
        secondScreenServiceActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SecondScreenServiceActivity.class);
                startActivity(intent);
            }
        });

        secondScreenServiceV2ActivityBtn = (Button) findViewById(R.id.secondScreenServiceV2ActivityBtn);
        secondScreenServiceV2ActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SecondScreenServiceV2Activity.class);
                startActivity(intent);
            }
        });

        receiptPrintingServiceActivityBtn = (Button) findViewById(R.id.receiptPrintingServiceActivityBtn);
        receiptPrintingServiceActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ReceiptPrintingServiceActivity.class);
                startActivity(intent);
            }
        });

        productServiceActivityBtn = (Button) findViewById(R.id.productServiceActivityBtn);
        productServiceActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProductServiceActivity.class);
                startActivity(intent);
            }
        });

        businessServiceActivityBtn = (Button) findViewById(R.id.businessServiceActivityBtn);
        businessServiceActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BusinessServiceActivity.class);
                startActivity(intent);
            }
        });

        billingServiceActivityBtn = (Button) findViewById(R.id.billingServiceActivityBtn);
        billingServiceActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InAppBillingActivity.class);
                startActivity(intent);
            }
        });

        accessoriesActivityBtn = (Button) findViewById(R.id.accessoriesActivityBtn);
        accessoriesActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AccessoriesActivity.class);
                startActivity(intent);
            }
        });

        cameraActivityBtn = (Button) findViewById(R.id.cameraActivityBtn);
        cameraActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        nonPaymentCardReaderActivityBtn = (Button) findViewById(R.id.nonPaymentCardReaderActivityBtn);
        nonPaymentCardReaderActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NonPaymentCardReaderActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

        return super.onOptionsItemSelected(item);
    }
}
