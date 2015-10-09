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
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import co.poynt.api.model.Order;
import co.poynt.api.model.OrderAmounts;
import co.poynt.api.model.OrderItem;
import co.poynt.api.model.Transaction;
import co.poynt.os.Constants;
import co.poynt.os.model.CapabilityProvider;
import co.poynt.os.model.CapabilityProviderFilter;
import co.poynt.os.model.CapabilityType;
import co.poynt.os.model.DiscountStatus;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.model.PaymentStatus;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntBusinessReadListener;
import co.poynt.os.services.v1.IPoyntBusinessService;
import co.poynt.os.services.v1.IPoyntCapabilityManager;
import co.poynt.os.services.v1.IPoyntCapabilityManagerListener;
import co.poynt.os.services.v1.IPoyntCustomDiscountService;
import co.poynt.os.services.v1.IPoyntCustomDiscountServiceListener;
import co.poynt.os.services.v1.IPoyntSecondScreenCheckInListener;
import co.poynt.os.services.v1.IPoyntSecondScreenCodeScanListener;
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
    private static final int DISCOUNT_REQUEST = 13133;
    private static final String TAG = "SampleActivity";

    private AccountManager accountManager;
    private IPoyntSessionService mSessionService;
    private IPoyntBusinessService mBusinessService;
    private IPoyntSecondScreenService mSecondScreenService;
    private IPoyntCapabilityManager mCapabilityManager;
    ProgressDialog progress;
    TextView mDumpTextView;
    TextView bizInfo;
    Button chargeBtn;
    ScrollView mScrollView;
    Account currentAccount = null;
    String userName;
    String accessToken;
    LinearLayout discountLayout;
    Button applyDiscount;
    EditText discountCode;
    Order sampleOrder;
    List<CapabilityServiceConnection> capabilityServiceConnections;

    Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        accountManager = AccountManager.get(this);
        gson = new GsonBuilder().setPrettyPrinting().create();

        bizInfo = (TextView) findViewById(R.id.bizInfo);
        chargeBtn = (Button) findViewById(R.id.chargeBtn);
        discountLayout = (LinearLayout) findViewById(R.id.discountLayout);
        discountCode = (EditText) findViewById(R.id.discountCode);
        applyDiscount = (Button) findViewById(R.id.applyDiscount);
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        sampleOrder = generateOrder();
        capabilityServiceConnections = new ArrayList<>();
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
                try {
                    // Android Account Manager does not maintain sessions - so we use Poynt Session
                    // Service to keep track of the current logged in user.
                    // NOTE that the access tokens are still managed by the Account Manager.
                    mSessionService.getCurrentUser(sessionServiceListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
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
                        mSecondScreenService.showItem(sampleOrder.getItems(),
                                sampleOrder.getAmounts().getSubTotal(), "USD");
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        Button displayWelcome = (Button) findViewById(R.id.displayWelcome);
        displayWelcome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * Request second screen service to display welcome screen w/ checkin button
                 * Enable second screen emulator from the developer options on your android
                 * emulator or device.
                 */
                try {
                    if (mSecondScreenService != null) {
                        mSecondScreenService.displayWelcome(secondScreenCheckInListener);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        final Button clearLog = (Button) findViewById(R.id.clearLog);
        clearLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLog();
            }
        });

        applyDiscount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = discountCode.getText().toString();
                List<String> discountCodes = new ArrayList<String>();
                discountCodes.add(code);
                Log.d(TAG, "Applying discount code: " + code);
                for (final CapabilityServiceConnection capabilityServiceConnection : capabilityServiceConnections) {
                    String requestId = UUID.randomUUID().toString();
                    String customerId = null;
                    try {
                        capabilityServiceConnection.getDiscountService().applyDiscount(
                                requestId, sampleOrder, null, discountCodes,
                                new IPoyntCustomDiscountServiceListener.Stub() {
                                    @Override
                                    public void onResponse(final DiscountStatus discountStatus, Transaction transaction, final Order order, String s) throws RemoteException {
                                        if (discountStatus.getCode() == DiscountStatus.Code.SUCCESS) {
                                            Log.d(TAG, "Applied discount: " + order.toString());
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    logData(gson.toJson(order));
                                                }
                                            });
                                        } else {
                                            Log.d(TAG, "Apply discount failed: " + discountStatus.getCode().name());
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    logData(discountStatus.getCode().name() + " --" + discountStatus.getMessage());
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onLaunchActivity(Intent intent, String s) throws RemoteException {
                                        Log.d(TAG, "Activity launch requested with intent: " + intent.toString());
                                        logData("Launching activity:" + intent.toString());
                                        startActivityForResult(intent, DISCOUNT_REQUEST);
                                    }
                                });
                    } catch (RemoteException e) {
                        Log.e(TAG, "Remote Exception received when applying discount", e);
                    }
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
        bindService(new Intent(IPoyntBusinessService.class.getName()),
                mBusinessServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(IPoyntSessionService.class.getName()),
                mSessionConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(IPoyntSecondScreenService.class.getName()),
                mSecondScreenConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(IPoyntCapabilityManager.class.getName()),
                mCapabilityManagerConnection, Context.BIND_AUTO_CREATE);
        // no need to bind as the binding happens after capability manager is connected
//        if (capabilityServiceConnections != null && capabilityServiceConnections.size() > 0) {
//            for (CapabilityServiceConnection serviceConnection : capabilityServiceConnections) {
//                serviceConnection.bind();
//            }
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "unbinding from services...");
        unbindService(mBusinessServiceConnection);
        unbindService(mSessionConnection);
        unbindService(mSecondScreenConnection);
        unbindService(mCapabilityManagerConnection);
        if (capabilityServiceConnections != null) {
            for (CapabilityServiceConnection serviceConnection : capabilityServiceConnections) {
//                serviceConnection.unBind();
            }
        }
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
                        logData("User: " + userName);
                        currentAccount = account;
                    } else {
                        logData("User: N/A");
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
                    logData(claimsStr.toString());
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

    /**
     * Class for interacting with the Poynt Capability Manager
     */
    private ServiceConnection mCapabilityManagerConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "IPoyntCapabilityManager is now connected");
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mCapabilityManager = IPoyntCapabilityManager.Stub.asInterface(service);
            // we are only interested in Discount providers
            CapabilityProviderFilter filter = new CapabilityProviderFilter(
                    CapabilityType.DISCOUNT,
                    null);
            try {
                mCapabilityManager.getCapabilityProviders(filter, poyntCapabilityManagerListener);
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to retrieve discount capabilities");
            }
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "IPoyntCapabilityManager has unexpectedly disconnected");
            mCapabilityManager = null;
        }
    };

    private IPoyntCapabilityManagerListener poyntCapabilityManagerListener
            = new IPoyntCapabilityManagerListener.Stub() {

        @Override
        public void onError(PoyntError poyntError) throws RemoteException {
            Log.e(TAG, "Failed connecting to discount capability providers");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logData("No Discount Capability providers found");
                }
            });
        }

        @Override
        public void onSuccess(final List<CapabilityProvider> providers) throws RemoteException {
            if (providers != null) {
                Log.d(TAG, "Got Discount capabilities: " + providers.size());
            } else {
                Log.d(TAG, "No Discount capabilities found");
            }

            StringBuilder providerStr = new StringBuilder();
            for (CapabilityProvider provider : providers) {
                // try to bind to providers
                providerStr.append("Provider:\n");
                providerStr.append("   Type [" + provider.getCapabilityType() + "]\n");
                providerStr.append("   Package Name [" + provider.getPackageName() + "]\n");
                // connect to each
                CapabilityServiceConnection capabilityServiceConnection =
                        new CapabilityServiceConnection(provider);
                capabilityServiceConnection.bind();
                // and add it for reference
                capabilityServiceConnections.add(capabilityServiceConnection);
            }
            final String providerToDisplay = providerStr.toString();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logData("Discount Providers found:");
                    logData(providerToDisplay);
                }
            });
        }
    };

    public class CapabilityServiceConnection implements ServiceConnection {

        CapabilityProvider capabilityProvider;
        IPoyntCustomDiscountService discountService;

        public CapabilityServiceConnection(CapabilityProvider capabilityProvider) {
            this.capabilityProvider = capabilityProvider;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        public IPoyntCustomDiscountService getDiscountService() {
            return discountService;
        }

        public CapabilityProvider getCapabilityProvider() {
            return capabilityProvider;
        }

        public void bind() {
            Log.d(TAG, "Binding to discount provider " + capabilityProvider.toString());
            Intent discountServiceIntent = new Intent();
            ComponentName component =
                    new ComponentName(capabilityProvider.getPackageName(),
                            capabilityProvider.getClassName());
            discountServiceIntent.setComponent(component);
            bindService(discountServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        public void unBind() {
            Log.d(TAG, "Unbinding from discount provider " + capabilityProvider.toString());
            if (serviceConnection != null) {
                unbindService(serviceConnection);
            }
        }

        private ServiceConnection serviceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "IPoyntCustomDiscountService is now connected");
                // Following the example above for an AIDL interface,
                // this gets an instance of the IRemoteInterface, which we can use to call on the service
                discountService = IPoyntCustomDiscountService.Stub.asInterface(service);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        discountLayout.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "IPoyntCustomDiscountService has unexpectedly disconnected");
                mSecondScreenService = null;
                serviceConnection = null;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        discountLayout.setVisibility(View.INVISIBLE);
                    }
                });
            }
        };
    }

    private void launchPoyntPayment(Long amount) {
        String currencyCode = NumberFormat.getCurrencyInstance().getCurrency().getCurrencyCode();

        Payment payment = new Payment();
        String referenceId = UUID.randomUUID().toString();
        payment.setReferenceId(referenceId);
        payment.setAmount(amount);
        payment.setCurrency(currencyCode);

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
            logData("Received onActivityResult from Payment Action");
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);
                    Log.d(TAG, "Received onPaymentAction from PaymentFragment w/ Status("
                            + payment.getStatus() + ")");
                    if (payment.getStatus().equals(PaymentStatus.COMPLETED)) {
                        logData("Payment Completed");
                    } else if (payment.getStatus().equals(PaymentStatus.AUTHORIZED)) {
                        logData("Payment Authorized");
                    } else if (payment.getStatus().equals(PaymentStatus.CANCELED)) {
                        logData("Payment Canceled");
                    } else if (payment.getStatus().equals(PaymentStatus.FAILED)) {
                        logData("Payment Failed");
                    } else if (payment.getStatus().equals(PaymentStatus.REFUNDED)) {
                        logData("Payment Refunded");
                    } else if (payment.getStatus().equals(PaymentStatus.VOIDED)) {
                        logData("Payment Voided");
                    } else {
                        logData("Payment Completed");
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                logData("Payment Canceled");
            }
        } else if (requestCode == DISCOUNT_REQUEST) {
            logData("Received onActivityResult from Discount Provider");
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    final Order order = data.getParcelableExtra("order");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (order != null) {
                                logData("Received order: " + order.toString());
                            } else {
                                logData("No ORDER RECEIVED");
                            }
                        }
                    });
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                logData("Failed to validate discount");
            }
        }
    }

    private Order generateOrder() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        List<OrderItem> items = new ArrayList<OrderItem>();
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
        order.setItems(items);

        BigDecimal subTotal = new BigDecimal(0);
        for (OrderItem item : items) {
            BigDecimal price = new BigDecimal(item.getUnitPrice());
            price.setScale(2, RoundingMode.HALF_UP);
            price = price.multiply(new BigDecimal(item.getQuantity()));
            subTotal = subTotal.add(price);
        }

        OrderAmounts amounts = new OrderAmounts();
        amounts.setCurrency("USD");
        amounts.setSubTotal(subTotal.longValue());
        order.setAmounts(amounts);

        return order;
    }

    public void logData(final String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDumpTextView.append("\n");
                mDumpTextView.append(data);
                mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
            }
        });
    }

    public void clearLog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // clear the output
                mDumpTextView.setText("");
                mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
            }
        });
    }

    private IPoyntSecondScreenCheckInListener secondScreenCheckInListener = new IPoyntSecondScreenCheckInListener.Stub() {
        @Override
        public void onCheckIn() throws RemoteException {
            logData("Customer check-in requested");
            mSecondScreenService.scanCode(new IPoyntSecondScreenCodeScanListener.Stub() {
                @Override
                public void onCodeScanned(String s) throws RemoteException {
                    logData("Code Scanned: " + s);
                    mSecondScreenService.displayWelcome(secondScreenCheckInListener);
                }

                @Override
                public void onCodeEntryCanceled() throws RemoteException {
                    logData("\nCode scan canceled");
                    mSecondScreenService.displayWelcome(secondScreenCheckInListener);
                }
            });
        }
    };
}
