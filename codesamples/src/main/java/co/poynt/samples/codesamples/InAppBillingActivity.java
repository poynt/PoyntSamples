package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.w3c.dom.Text;

import java.util.UUID;

import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntInAppBillingService;
import co.poynt.os.services.v1.IPoyntInAppBillingServiceListener;

import static android.view.View.GONE;


public class InAppBillingActivity extends Activity {

    private static final String TAG = InAppBillingActivity.class.getSimpleName();
    private static final int BUY_INTENT_REQUEST_CODE = 98765;
    private Button checkSubscriptionBtn;
    private Button launchBillingFragment;
    private Button getPlansBtn;
    private Button replaceSubscription;
    private Button restoreSubscription;
    private TextView mDumpTextView;
    private ScrollView mScrollView;
    private TextView status;

    IPoyntInAppBillingService mBillingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_billing);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        status = (TextView) findViewById(R.id.status);
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

        getPlansBtn = (Button) findViewById(R.id.getPlansBtn);
        // hide it until we are connected to inapp billing service
        getPlansBtn.setVisibility(GONE);
        getPlansBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBillingService != null) {
                    logReceivedMessage("Sending GetPlans()");
                    getPlans();
                } else {
                    Log.d(TAG, "NOT CONNECTED TO INAPP-BILLING");
                    logReceivedMessage("Not Connected to inApp Billing Service");
                    getPlansBtn.setVisibility(GONE);
                }
            }
        });
        launchBillingFragment = (Button) findViewById(R.id.launchBillingFragment);
        launchBillingFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBillingService != null) {
                    logReceivedMessage("Requesting billing intent...");
                    try {
                        // launch billing for $0 plan - add as a new subscription
                        Bundle bundle = getBillingFragmentIntent("f2e9001d-6d51-4e74-888c-c77b9a0ff666", false);
                        if (bundle != null && bundle.containsKey("BUY_INTENT")) {
                            PendingIntent intent = bundle.getParcelable("BUY_INTENT");
                            if (intent != null) {
                                try {
                                    startIntentSenderForResult(
                                            intent.getIntentSender(),
                                            BUY_INTENT_REQUEST_CODE,
                                            null,
                                            Integer.valueOf(0),
                                            Integer.valueOf(0),
                                            Integer.valueOf(0));
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                    logReceivedMessage("Failed to launch billing fragment!");
                                }
                            } else {
                                logReceivedMessage("Did not receive buy intent!");
                            }
                        } else {
                            logReceivedMessage("Failed to obtain billing fragment intent!");
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.d(TAG, "NOT CONNECTED TO INAPP-BILLING");
                    logReceivedMessage("Not Connected to inApp Billing Service");
                    launchBillingFragment.setVisibility(GONE);
                }
            }
        });

        replaceSubscription = (Button) findViewById(R.id.replaceSubscription);
        replaceSubscription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBillingService != null) {
                    logReceivedMessage("Requesting billing intent...");
                    try {
                        // launch billing fragment for $5 plan - replace existing one
                        Bundle bundle = getBillingFragmentIntent("01e2a28f-42c4-4efc-a4f2-4e0c4e4b842e", true);
                        if (bundle != null && bundle.containsKey("BUY_INTENT")) {
                            PendingIntent intent = bundle.getParcelable("BUY_INTENT");
                            if (intent != null) {
                                try {
                                    startIntentSenderForResult(
                                            intent.getIntentSender(),
                                            BUY_INTENT_REQUEST_CODE,
                                            null,
                                            Integer.valueOf(0),
                                            Integer.valueOf(0),
                                            Integer.valueOf(0));
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                    logReceivedMessage("Failed to launch billing fragment!");
                                }
                            } else {
                                logReceivedMessage("Did not receive buy intent!");
                            }
                        } else {
                            logReceivedMessage("Failed to obtain billing fragment intent!");
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.d(TAG, "NOT CONNECTED TO INAPP-BILLING");
                    logReceivedMessage("Not Connected to inApp Billing Service");
                    launchBillingFragment.setVisibility(GONE);
                }
            }
        });

        restoreSubscription = (Button) findViewById(R.id.restoreSubscription);
        restoreSubscription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBillingService != null) {
                    logReceivedMessage("Requesting billing intent...");
                    try {
                        // launch billing fragment for $5 plan - replace existing one
                        Bundle bundle = getBillingFragmentIntent("23c76c0d-9fcd-49fa-b225-fee20ef53152");
                        if (bundle != null && bundle.containsKey("BUY_INTENT")) {
                            PendingIntent intent = bundle.getParcelable("BUY_INTENT");
                            if (intent != null) {
                                try {
                                    startIntentSenderForResult(
                                            intent.getIntentSender(),
                                            BUY_INTENT_REQUEST_CODE,
                                            null,
                                            Integer.valueOf(0),
                                            Integer.valueOf(0),
                                            Integer.valueOf(0));
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                    logReceivedMessage("Failed to launch billing fragment!");
                                }
                            } else {
                                logReceivedMessage("Did not receive buy intent!");
                            }
                        } else {
                            logReceivedMessage("Failed to obtain billing fragment intent!");
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.d(TAG, "NOT CONNECTED TO INAPP-BILLING");
                    logReceivedMessage("Not Connected to inApp Billing Service");
                    launchBillingFragment.setVisibility(GONE);
                }
            }
        });
    }

    private Bundle getBillingFragmentIntent(String planId, boolean replace) throws RemoteException {
        Bundle bundle = new Bundle();
        // add plan Id
        bundle.putString("plan_id", planId);
        bundle.putBoolean("replace", replace);
        return mBillingService.getBillingIntent(getPackageName(), bundle);
    }

    private Bundle getBillingFragmentIntent(String subscriptionId) throws RemoteException {
        Bundle bundle = new Bundle();
        // add subscriptionId
        bundle.putString("subscription_id", subscriptionId);
        bundle.putBoolean("restore", true);
        return mBillingService.getBillingIntent(getPackageName(), bundle);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == BUY_INTENT_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                logReceivedMessage("Subscription request was successful - run Check Subscription to confirm!");
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Result canceled");
                logReceivedMessage("Subscription request failed!");
            }
        }
    }

    private void getPlans() {
        try {
            if (mBillingService != null) {
                Log.d(TAG, "calling getPlans()");
                String requestId = UUID.randomUUID().toString();
                mBillingService.getPlans("co.poynt.samples.codesamples", requestId,
                        new IPoyntInAppBillingServiceListener.Stub() {
                            @Override
                            public void onResponse(final String resultJson, final PoyntError poyntError, String requestId)
                                    throws RemoteException {
                                Log.d(TAG, "Received response from InAppBillingService for " +
                                        "getPlans(" + requestId + ")");
//                                Log.d(TAG, "response: " + resultJson);
                                if (poyntError != null) {
                                    Log.d(TAG, "poyntError: " + poyntError.toString());
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (poyntError != null) {
                                            logReceivedMessage("Failed to obtain plans: "
                                                    + poyntError.toString());
                                        } else {
                                            JsonParser parser = new JsonParser();
                                            JsonObject json = parser.parse(resultJson).getAsJsonObject();
                                            JsonArray plans = json.getAsJsonArray("list");
                                            if (plans.size() > 0){
                                                setStatus(status, "PLANS RECEIVED");
                                            }
                                            logReceivedMessage("Result for get plans: "
                                                    + toPrettyFormat(resultJson));
                                        }
                                    }
                                });
                            }
                        });

            } else {
                Log.e(TAG, "Not connected to InAppBillingService!");
            }
        } catch (SecurityException e) {
            logReceivedMessage(e.getMessage());
        } catch (RemoteException e) {
            e.printStackTrace();
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
                                                    + toPrettyFormat(resultJson));
                                            setStatus(status, "SUBSCRIPTION RESPONSE RECEIVED");

                                        }
                                    }
                                });
                            }
                        });

            } else {
                Log.e(TAG, "Not connected to InAppBillingService!");
            }
        } catch (SecurityException e) {
            logReceivedMessage(e.getMessage());
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
                    getPlansBtn.setVisibility(View.VISIBLE);
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

    public static String toPrettyFormat(String jsonString) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(jsonString).getAsJsonObject();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = gson.toJson(json);

        return prettyJson;
    }

    private void setStatus(final TextView textView, final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(msg);
            }
        });
    }
}
