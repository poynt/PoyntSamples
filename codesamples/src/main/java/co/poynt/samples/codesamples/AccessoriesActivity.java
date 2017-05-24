package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import co.poynt.os.model.AccessoryProvider;
import co.poynt.os.model.AccessoryProviderFilter;
import co.poynt.os.model.AccessoryType;
import co.poynt.os.model.CashDrawerStatus;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntAccessoryManagerListener;
import co.poynt.os.services.v1.IPoyntCashDrawerService;
import co.poynt.os.services.v1.IPoyntCashDrawerServiceListener;
import co.poynt.os.util.AccessoryProviderServiceHelper;

public class AccessoriesActivity extends Activity {

    private static final String TAG = AccessoriesActivity.class.getSimpleName();

    private TextView mDumpTextView;
    private ScrollView mScrollView;

    private AccessoryProviderServiceHelper accessoryProviderServiceHelper;
    private HashMap<AccessoryProvider, IBinder> mCashDrawerServices = new HashMap<>();
    private List<AccessoryProvider> providers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessories);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);

        Button openCashDrawer = (Button) findViewById(R.id.openCashDrawer);
        openCashDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCashDrawer();
            }
        });

        try {
            // initialize capabilityProviderServiceHelper
            accessoryProviderServiceHelper = new AccessoryProviderServiceHelper(this);
            // connect to accessory manager service
            accessoryProviderServiceHelper.bindAccessoryManager(
                    new AccessoryProviderServiceHelper.AccessoryManagerConnectionCallback() {
                        @Override
                        public void onConnected(AccessoryProviderServiceHelper accessoryProviderServiceHelper) {
                            // when connected check if we have any cash drawers registered
                            if (accessoryProviderServiceHelper.getAccessoryServiceManager() != null) {
                                AccessoryProviderFilter filter = new AccessoryProviderFilter(AccessoryType.CASH_DRAWER);
                                Log.d(TAG, "trying to get CASH DRAWER accessory...");
                                try {
                                    // look up the cash drawers using the filter
                                    accessoryProviderServiceHelper.getAccessoryServiceManager().getAccessoryProviders(
                                            filter, poyntAccessoryManagerListener);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Unable to connect to Accessory Service", e);
                                    logReceivedMessage("Unable to connect to Accessory Service");
                                }
                            } else {
                                logReceivedMessage("Not connected with accessory service manager");
                            }
                        }

                        @Override
                        public void onDisconnected(AccessoryProviderServiceHelper accessoryProviderServiceHelper) {
                            logReceivedMessage("Disconnected with accessory service manager");
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to connect to capability or accessory manager", e);
            logReceivedMessage("Failed to connect to capability or accessory manager");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    // this is the accessory manager listener which gets invoked when accessory manager completes
    // scanning for the requested accessories
    private IPoyntAccessoryManagerListener poyntAccessoryManagerListener
            = new IPoyntAccessoryManagerListener.Stub() {

        @Override
        public void onError(PoyntError poyntError) throws RemoteException {
            Log.e(TAG, "Failed to connect to accessory manager: " + poyntError);
        }

        @Override
        public void onSuccess(final List<AccessoryProvider> cashDrawers) throws RemoteException {
            // now that we are connected - request service connections to each accessory provider
            if (cashDrawers != null && cashDrawers.size() > 0) {
                // save it for future reference
                providers = cashDrawers;
                if (accessoryProviderServiceHelper.getAccessoryServiceManager() != null) {
                    // for each cash drawer accessory - request "service" connections if it's still connected
                    for (AccessoryProvider cashDrawer : cashDrawers) {
                        Log.d(TAG, "Cashdrawer: " + cashDrawer.toString());
                        if (cashDrawer.isConnected()) {
                            // request service connection binder
                            // IMP: note that this method returns service connection if it already exists
                            // hence the need for both connection callback and the returned value
                            IBinder binder = accessoryProviderServiceHelper.getAccessoryService(
                                    cashDrawer, AccessoryType.CASH_DRAWER,
                                    providerConnectionCallback);
                            //already cached connection.
                            if (binder != null) {
                                mCashDrawerServices.put(cashDrawer, binder);
                            }
                        }
                    }
                }
            } else {
                logReceivedMessage("No Cash Drawers found");
            }
        }
    };

    // this is the callback for the service connection to each accessory provider service in this case
    // the android service supporting cash drawer accessory
    private AccessoryProviderServiceHelper.ProviderConnectionCallback providerConnectionCallback
            = new AccessoryProviderServiceHelper.ProviderConnectionCallback() {

        @Override
        public void onConnected(AccessoryProvider provider, IBinder binder) {
            // in some cases multiple accessories of the same type (eg. two cash drawers of same
            // make/model or two star printers) might be supported by the same android service
            // so here we check if we need to share the same service connection for more than
            // one accessory provider
            List<AccessoryProvider> otherProviders = findMatchingProviders(provider);
            // all of them share the same service binder
            for (AccessoryProvider matchedProvider : otherProviders) {
                mCashDrawerServices.put(matchedProvider, binder);
            }
        }

        @Override
        public void onDisconnected(AccessoryProvider provider, IBinder binder) {
            // set the lookup done to false so we can try looking up again if needed
            if (mCashDrawerServices != null && mCashDrawerServices.size() > 0) {
                mCashDrawerServices.remove(binder);
                // try to renew the connection.
                if (accessoryProviderServiceHelper.getAccessoryServiceManager() != null) {
                    IBinder binder2 = accessoryProviderServiceHelper.getAccessoryService(
                            provider, AccessoryType.CASH_DRAWER,
                            providerConnectionCallback);
                    if (binder2 != null) {//already cached connection.
                        mCashDrawerServices.put(provider, binder2);
                    }
                }
            }
        }
    };

    // we do this if there are multiple accessories connected of the same type/provider
    private List<AccessoryProvider> findMatchingProviders(AccessoryProvider provider) {
        ArrayList<AccessoryProvider> matchedProviders = new ArrayList<>();
        if (providers != null) {
            for (AccessoryProvider printer : providers) {
                if (provider.getAccessoryType() == printer.getAccessoryType()
                        && provider.getPackageName().equals(printer.getPackageName())
                        && provider.getClassName().equals(printer.getClassName())) {
                    matchedProviders.add(printer);
                }
            }
        }
        return matchedProviders;
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

    private void openCashDrawer() {
        // IMP: here we are opening all connected cash drawers - but typically there should be
        // an option offered to the merchant to select which cash drawer to open. For now that's the
        // app's responsibility
        if (providers != null && providers.size() > 0) {
            logReceivedMessage("Opening cash drawer...");
            for (AccessoryProvider provider : providers) {
                try {
                    IBinder binder = mCashDrawerServices.get(provider);
                    if (binder != null) {
                        IPoyntCashDrawerService drawerService = IPoyntCashDrawerService.Stub.asInterface(binder);
                        if (drawerService != null) {
                            Log.d(TAG, "Opening Cash drawer");
                            drawerService.openDrawerByName(
                                    provider.getProviderName(),
                                    "open",
                                    new IPoyntCashDrawerServiceListener.Stub() {
                                        @Override
                                        public void onResponse(CashDrawerStatus status, String requestId) throws RemoteException {
                                            if (status != null &&
                                                    (status.getCode() == CashDrawerStatus.Code.DISCONNECTED
                                                            || status.getCode() == CashDrawerStatus.Code.ERROR
                                                    )) {
                                                logReceivedMessage("Failed to open Cash Drawer");
                                            }
                                        }
                                    });
                        }
                    } else {
                        logReceivedMessage("No service connection found");
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to communicate with cash drawer", e);
                    logReceivedMessage("Failed to communicate with cash drawer");
                }
            }
        } else {
            Log.e(TAG, "cash drawer not connected");
            logReceivedMessage("No connected cash drawers found");
        }
    }
}
