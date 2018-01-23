package co.poynt.samples;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.poynt.api.model.Business;
import co.poynt.api.model.OrderItem;
import co.poynt.os.Constants;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.model.PaymentStatus;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntBusinessReadListener;
import co.poynt.os.services.v1.IPoyntBusinessService;
import co.poynt.os.services.v1.IPoyntSecondScreenService;
import co.poynt.os.services.v1.IPoyntSessionService;
import co.poynt.os.services.v1.IPoyntSessionServiceListener;

/**
 * A simple sample app demonstrating how to get business info from the device using
 * the PoyntBusinessService.
 */
public class SampleActivity extends Activity {

    private static final int AUTHORIZATION_CODE = 1993;
    // request code for payment service activity
    private static final int COLLECT_PAYMENT_REQUEST = 13132;
    private static final String TAG = "SampleActivity";

    private AccountManager accountManager;
    private IPoyntSessionService mSessionService;
    private IPoyntBusinessService mBusinessService;
    private IPoyntSecondScreenService mSecondScreenService;
    ProgressDialog progress;
    private TextView bizInfo;
    private Button chargeBtn;
    Account currentAccount = null;
    String userName;
    String accessToken;
    TextView tokenInfo;
    TextView userInfo;
    List<OrderItem> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        accountManager = AccountManager.get(this);
        tokenInfo = (TextView) findViewById(R.id.tokenInfo);
        userInfo = (TextView) findViewById(R.id.userInfo);
        bizInfo = (TextView) findViewById(R.id.bizInfo);
        chargeBtn = (Button) findViewById(R.id.chargeBtn);

        chargeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPoyntPayment(1000l);
            }
        });
        Button currentUser = (Button) findViewById(R.id.currentUser);
        currentUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                try {
//                    // Android Account Manager does not maintain sessions - so we use Poynt Session
//                    // Service to keep track of the current logged in user.
//                    // NOTE that the access tokens are still managed by the Account Manager.
//                    mSessionService.getCurrentUser(sessionServiceListener);
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
                Intent intent = new Intent("poynt.intent.action.VIEW_TRANSACTIONS");
                startActivity(intent);

            }
        });

        Button getToken = (Button) findViewById(R.id.getToken);
        getToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get access token for the current user from tha account manager
                // Note the authTokenType passed
                if (currentAccount != null) {
                    accountManager.getAuthToken(currentAccount, Constants.Accounts.POYNT_AUTH_TOKEN,
                            null, SampleActivity.this, new OnTokenAcquired(), null);
                } else {
                    // launch the login
                    accountManager.getAuthToken(Constants.Accounts.POYNT_UNKNOWN_ACCOUNT,
                            Constants.Accounts.POYNT_AUTH_TOKEN, null, SampleActivity.this,
                            new OnTokenAcquired(), null);
                }
            }
        });

        // create some dummy items to display in second screen
        items = new ArrayList<OrderItem>();
        OrderItem item1 = new OrderItem();
        // these are the only required fields for second screen display
        item1.setName("Item1");
        item1.setUnitPrice(100l);
        item1.setQuantity(1.0f);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        // these are the only required fields for second screen display
        item2.setName("Item2");
        item2.setUnitPrice(100l);
        item2.setQuantity(1.0f);
        items.add(item2);

        OrderItem item3 = new OrderItem();
        // these are the only required fields for second screen display
        item3.setName("Item3");
        item3.setUnitPrice(100l);
        item3.setQuantity(2.0f);
        items.add(item3);

        Button displayItems = (Button) findViewById(R.id.displayItems);
        displayItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 * Request second screen service to display the items to the consumer
                 * Enable second screen emulator from the developer options on your android
                 * emulator or device.
                 */
                try {
                    if (mSecondScreenService != null) {
                        BigDecimal total = new BigDecimal(0);
                        for (OrderItem item : items) {
                            BigDecimal price = new BigDecimal(item.getUnitPrice());
                            price.setScale(2, RoundingMode.HALF_UP);
                            price = price.multiply(new BigDecimal(item.getQuantity()));
                            total = total.add(price);
                        }
                        mSecondScreenService.showItem(items, total.longValue(), "USD");
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "binding to services...");
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_BUSINESS_SERVICE),
                mBusinessServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_SESSION_SERVICE),
                mSessionConnection, Context.BIND_AUTO_CREATE);
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_SECOND_SCREEN_SERVICE),
                mSecondScreenConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "unbinding from services...");
        unbindService(mBusinessServiceConnection);
        unbindService(mSessionConnection);
        unbindService(mSecondScreenConnection);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    /**
     * Class for interacting with the BusinessService
     */
    private ServiceConnection mBusinessServiceConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "PoyntBusinessService is now connected");
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mBusinessService = IPoyntBusinessService.Stub.asInterface(service);

            // first load business and business users to make sure the device resolves to a business
            // invoke the api to get business details
            try {
                mBusinessService.getBusiness(businessReadServiceListener);
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to connect to business service to resolve the business this terminal belongs to!");
            }
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "PoyntBusinessService has unexpectedly disconnected");
            mBusinessService = null;
        }
    };

    /**
     * Business service listener interface
     */
    private IPoyntBusinessReadListener businessReadServiceListener = new IPoyntBusinessReadListener.Stub() {
        @Override
        public void onResponse(final Business business, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "Received business obj:" + business.getDoingBusinessAs() + " -- " + business.getDescription());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bizInfo.setText(business.getDoingBusinessAs());
                    chargeBtn.setVisibility(View.VISIBLE);
                }
            });
        }
    };

    /**
     * Class for interacting with the SessionService
     */
    private ServiceConnection mSessionConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.e("TransactionTestActivity", "PoyntSessionService is now connected");
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mSessionService = IPoyntSessionService.Stub.asInterface(service);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e("TransactionTestActivity", "PoyntSessionService has unexpectedly disconnected");
            mSessionService = null;
        }
    };

    /**
     * Session service listener interface
     */
    private IPoyntSessionServiceListener sessionServiceListener = new IPoyntSessionServiceListener.Stub() {

        @Override
        public void onResponse(final Account account, PoyntError poyntError) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (account != null) {
                        userName = account.name;
                        userInfo.setText(userName);
                        currentAccount = account;
                    } else {
                        userInfo.setText("n/a");
                        currentAccount = null;
                        userName = null;
                    }
                }
            });
        }
    };


    /**
     * Account manager callback handler to receive access token or launch
     * login activity if requested by the Poynt authenticator
     */
    public class OnTokenAcquired implements AccountManagerCallback<Bundle> {

        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                if (progress != null) {
                    progress.dismiss();
                }
                Bundle bundle = result.getResult();

                Intent launch = (Intent) bundle.get(AccountManager.KEY_INTENT);
                if (launch != null) {
                    Log.d("TransactionTestActivity", "received intent to login");
                    startActivityForResult(launch, AUTHORIZATION_CODE);
                } else {
                    Log.d("TransactionTestActivity", "token user:" + bundle.get(AccountManager.KEY_ACCOUNT_NAME));
                    accessToken = bundle
                            .getString(AccountManager.KEY_AUTHTOKEN);
                    Log.d("TransactionTestActivity", "received token result: " + accessToken);
                    // display the claims in the screen
                    SignedJWT signedJWT = SignedJWT.parse(accessToken);
                    StringBuilder claimsStr = new StringBuilder();
                    ReadOnlyJWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                    claimsStr.append("Subject: " + claims.getSubject());
                    claimsStr.append(", Type: " + claims.getType());
                    claimsStr.append(", Issuer: " + claims.getIssuer());
                    claimsStr.append(", JWT ID: " + claims.getJWTID());
                    claimsStr.append(", IssueTime : " + claims.getIssueTime());
                    claimsStr.append(", Expiration Time: " + claims.getExpirationTime());
                    claimsStr.append(", Not Before Time: " + claims.getNotBeforeTime());
                    Map<String, Object> customClaims = claims.getCustomClaims();
                    for (Map.Entry<String, Object> entry : customClaims.entrySet()) {
                        claimsStr.append(", " + entry.getKey() + ": " + entry.getValue());
                    }
                    tokenInfo.setText(claimsStr.toString());

                }
            } catch (Exception e) {
                Log.d("TransactionTestActivity", "Exception received: " + e.getMessage());
            }
        }
    }

    /**
     * Class for interacting with the Second Screen Service
     */
    private ServiceConnection mSecondScreenConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "IPoyntSecondScreenService is now connected");
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mSecondScreenService = IPoyntSecondScreenService.Stub.asInterface(service);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "IPoyntSecondScreenService has unexpectedly disconnected");
            mSecondScreenService = null;
        }
    };

    private void launchPoyntPayment(Long amount) {
        String currencyCode = NumberFormat.getCurrencyInstance().getCurrency().getCurrencyCode();

        Payment payment = new Payment();
        String referenceId = UUID.randomUUID().toString();
        payment.setReferenceId(referenceId);
        payment.setAmount(amount);
        payment.setCurrency(currencyCode);

        // default to cash if the device has no card reader
        if (!isPoyntTerminal()) {
            payment.setCashOnly(true);
        }

        // start Payment activity for result
        try {
            Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
            collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, payment);
            startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_REQUEST);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Poynt Payment Activity not found - did you install PoyntServices?", ex);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "Received onActivityResult (" + requestCode + ")");
        // Check which request we're responding to
        if (requestCode == COLLECT_PAYMENT_REQUEST) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);
                    Log.d(TAG, "Received onPaymentAction from PaymentFragment w/ Status("
                            + payment.getStatus() + ")");
                    if (payment.getStatus().equals(PaymentStatus.COMPLETED)) {
                        Toast.makeText(this, "Payment Completed", Toast.LENGTH_LONG).show();
                    } else if (payment.getStatus().equals(PaymentStatus.AUTHORIZED)) {
                        Toast.makeText(this, "Payment Authorized", Toast.LENGTH_LONG).show();
                    } else if (payment.getStatus().equals(PaymentStatus.CANCELED)) {
                        Toast.makeText(this, "Payment Canceled", Toast.LENGTH_LONG).show();
                    } else if (payment.getStatus().equals(PaymentStatus.FAILED)) {
                        Toast.makeText(this, "Payment Failed", Toast.LENGTH_LONG).show();
                    } else if (payment.getStatus().equals(PaymentStatus.REFUNDED)) {
                        Toast.makeText(this, "Payment Refunded", Toast.LENGTH_LONG).show();
                    } else if (payment.getStatus().equals(PaymentStatus.VOIDED)) {
                        Toast.makeText(this, "Payment Voided", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Payment Completed", Toast.LENGTH_LONG).show();
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Payment Canceled", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * @return true is the app is running on a Poynt terminal and has a card reader
     */
    private boolean isPoyntTerminal() {
        return "POYNT".equals(Build.MANUFACTURER);
    }

}
