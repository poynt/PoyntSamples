package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;


public class ScannerActivity extends Activity {

    private static final String TAG = ScannerActivity.class.getSimpleName();
    private static final int SCANNER_REQUEST_CODE = 46576;
    private Button scanCode;
    private TextView mDumpTextView;
    private ScrollView mScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);

        scanCode = (Button) findViewById(R.id.scanCode);
        scanCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // launch bar code scanner
                Intent intent = new Intent("poynt.intent.action.SCANNER");
                // "MULTI" or "SINGLE"
                intent.putExtra("MODE", "SINGLE");
                // if multi mode - also register the receiver
                IntentFilter scannerIntentFilter = new IntentFilter();
                scannerIntentFilter.addAction("poynt.intent.action.SCANNER_RESULT");
                registerReceiver(scanResultReceiver, scannerIntentFilter);
                startActivityForResult(intent, SCANNER_REQUEST_CODE);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == SCANNER_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // get scan results
                String code = data.getStringExtra("CODE");
                String format = data.getStringExtra("FORMAT");
                logReceivedMessage("Scanner request was successful - Code:"
                        + code + " Format:" + format);
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Result canceled");
                logReceivedMessage("Scanner canceled!");
            }
            // always unregister when it's done
            unregisterReceiver(scanResultReceiver);
        }
    }


    public void logReceivedMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDumpTextView.append("<< " + message + "\n\n");
                mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
            }
        });
    }

    private void clearLog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDumpTextView.setText("");
                mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
            }
        });
    }

    BroadcastReceiver scanResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String code = intent.getStringExtra("CODE");
            String format = intent.getStringExtra("FORMAT");
            logReceivedMessage("Scanner request was successful - Code:"
                    + code + " Format:" + format);
        }
    };
}
