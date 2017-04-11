package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.UUID;

import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntInAppBillingService;
import co.poynt.os.services.v1.IPoyntInAppBillingServiceListener;

import static android.view.View.GONE;


public class InAppBillingActivity extends Activity {

    private static final String TAG = InAppBillingActivity.class.getSimpleName();
    private Button checkSubscriptionBtn;
    private TextView mDumpTextView;
    private ScrollView mScrollView;

    IPoyntInAppBillingService mBillingService;

    private static final ComponentName COMPONENT_POYNT_INAPP_BILLING_SERVICE = new ComponentName("com.poynt.store", "co.poynt.os.services.v1.IPoyntInAppBillingService");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_billing);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);

        checkSubscriptionBtn = (Button) findViewById(R.id.checkSubscriptionBtn);
        // hide it until we are connected to inapp billing service
        checkSubscriptionBtn.setVisibility(GONE);
        checkSubscriptionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBillingService != null) {
                    logReceivedMessage("Sending GetSubscriptions()");
                    checkSubscriptionStatus();
                } else {
                    Log.d(TAG, "NOT CONNECTED TO INAPP-BILLING");
                    logReceivedMessage("Not Connected to inApp Billing Service");
                    checkSubscriptionBtn.setVisibility(GONE);
                }
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
    protected void onResume() {
        super.onResume();
//        bindService(Intents.getComponentIntent(COMPONENT_POYNT_INAPP_BILLING_SERVICE),
//                mServiceConn, Context.BIND_AUTO_CREATE);
        Intent serviceIntent =
                new Intent("com.poynt.store.PoyntInAppBillingService.BIND");
        serviceIntent.setPackage("com.poynt.store");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mBillingService != null) {
            unbindService(mServiceConn);
        }
    }


    private void checkSubscriptionStatus() {
        try {
            if (mBillingService != null) {
                Log.d(TAG, "calling checkSubscriptionStatus()");
                String requestId = UUID.randomUUID().toString();
                mBillingService.getSubscriptions("co.poynt.samples.codesamples", requestId,
                        new IPoyntInAppBillingServiceListener.Stub() {
                            @Override
                            public void onResponse(final String resultJson, final PoyntError poyntError, String requestId)
                                    throws RemoteException {
                                Log.d(TAG, "Received response from InAppBillingService for " +
                                        "getSubscriptions(" + requestId + ")");
//                                Log.d(TAG, "response: " + resultJson);
                                if (poyntError != null) {
                                    Log.d(TAG, "poyntError: " + poyntError.toString());
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (poyntError != null) {
                                            logReceivedMessage("Failed to obtain subscriptions: "
                                                    + poyntError.toString());
                                        } else {
                                            logReceivedMessage("Result for get subscriptions: "
                                                    + resultJson);
                                        }
                                    }
                                });
                            }
                        });

            } else {
                Log.e(TAG, "Not connected to InAppBillingService!");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from InAppBilling");
            mBillingService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            Log.d(TAG, "Connected to InAppBilling");
            mBillingService = IPoyntInAppBillingService.Stub.asInterface(service);
            // enable button to test subscriptions
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    checkSubscriptionBtn.setVisibility(View.VISIBLE);
                }
            });
        }
    };

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
}
