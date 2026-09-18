package co.poynt.samples.codesamples;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import co.poynt.api.model.Business;
import co.poynt.api.model.CardType;
import co.poynt.api.model.Store;
import co.poynt.os.Constants;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntBusinessReadListener;
import co.poynt.os.services.v1.IPoyntBusinessService;
import co.poynt.os.services.v1.IPoyntConfigurationService;
import co.poynt.os.services.v1.IPoyntConfigurationUpdateListener;
import co.poynt.os.services.v1.IPoyntDeviceInfoListener;
import co.poynt.os.services.v1.IPoyntReaderVersionListener;
import co.poynt.os.services.v1.IPoyntSimCardInfoListener;

public class ConfigurationServiceActivity extends Activity {

    private static final String TAG = "ConfigServiceActivity";

    private IPoyntConfigurationService configurationService;
    private IPoyntBusinessService businessService;

    private Button getSimInfoButton;
    private Button getFirmwareComponentVersBtn;
    private Button getDeviceInfoButton;
    private Spinner customerChoiceSpinner;
    private Button enableCustomerChoiceButton;
    private TextView infoTextView;

    private Business business;
    private String mid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration_service);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        getSimInfoButton = findViewById(R.id.getSimInfoButton);
        getFirmwareComponentVersBtn = findViewById(R.id.getFirmwareCompVersButton);
        getDeviceInfoButton = findViewById(R.id.getDeviceInfoButton);
        customerChoiceSpinner = findViewById(R.id.customerChoiceSpinner);
        enableCustomerChoiceButton = findViewById(R.id.enableCustomerChoiceButton);
        infoTextView = findViewById(R.id.infoTextView);

        getSimInfoButton.setOnClickListener(view -> {
            try {
                configurationService.getSimCardInfo(new IPoyntSimCardInfoListener.Stub() {
                    @Override
                    public void onSimCardInfoReceived(Bundle bundle) throws RemoteException {
                        runOnUiThread(() -> {
                            StringBuilder stringBuilder = new StringBuilder("Sim Card Info:\n");
                            for (String key : bundle.keySet()) {
                                stringBuilder.append(key).append(" : ").append(bundle.get(key)).append("\n");
                            }
                            infoTextView.setText(stringBuilder);
                        });
                    }
                });
            } catch (Exception e) {
                infoTextView.setText("Exception: " + e.getMessage());
            }
        });

        getFirmwareComponentVersBtn.setOnClickListener(v -> {
            try {
                configurationService.getFirmwareComponentVersion(new IPoyntReaderVersionListener.Stub() {
                    @Override
                    public void onSuccess(String fwVersion) throws RemoteException {
                        runOnUiThread(() -> {
                            Log.d("FW Component version",
                                    fwVersion.replaceAll("(\\r|\\n)", ""));
                            infoTextView.setText(fwVersion);
                        });
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onFailure(PoyntError poyntError) throws RemoteException {
                        if (poyntError != null) {
                            runOnUiThread(() -> {
                                infoTextView.setText("Error: " + poyntError.toString());
                            });
                        }
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        getDeviceInfoButton.setOnClickListener(view -> {
            try {
                configurationService.getDeviceInfo(new IPoyntDeviceInfoListener.Stub() {

                    @Override
                    public void onDeviceInfoReceived(Bundle bundle) throws RemoteException {
                        runOnUiThread(() -> {
                            StringBuilder stringBuilder = new StringBuilder("Device Info:\n");
                            for (String key : bundle.keySet()) {
                                stringBuilder.append(key).append(" : ").append(bundle.get(key)).append("\n");
                            }
                            infoTextView.setText(stringBuilder);
                        });
                    }
                });
            } catch (Exception e) {
                infoTextView.setText("Exception: " + e.getMessage());
            }
        });

        enableCustomerChoiceButton.setOnClickListener(view ->
                onEnableCustomerChoiceClicked(customerChoiceSpinner.getSelectedItemPosition()));
    }

    private void initializeBusinessAndMid() {
        if (businessService == null) {
            Log.d(TAG, "initializeBusinessAndMid: businessService not connected yet");
            return;
        }
        try {
            businessService.getBusiness(new IPoyntBusinessReadListener.Stub() {
                @Override
                public void onResponse(Business loadedBusiness, PoyntError poyntError) throws RemoteException {
                    business = loadedBusiness;
                    mid = null;
                    if (business != null && business.getStores() != null && !business.getStores().isEmpty()) {
                        // getBusiness() returns only the store this terminal is activated against
                        Store store = business.getStores().get(0);
                        mid = store != null ? store.getExternalStoreId() : null;
                    }
                    Log.d(TAG, "initializeBusinessAndMid: mid=" + mid
                            + (poyntError != null ? " error=" + poyntError : ""));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "initializeBusinessAndMid failed", e);
        }
    }

    @SuppressLint("SetTextI18n")
    private void onEnableCustomerChoiceClicked(int selectedPosition) {
        final List<String> defaultSchemes;
        final List<String> alternativeSchemes;
        // here anyone may try many different combinations:
        switch (selectedPosition) {
            case 0:
                defaultSchemes = Collections.singletonList(CardType.BANCOMAT.name());
                alternativeSchemes = Arrays.asList(CardType.VISA.name(), CardType.MASTERCARD.name());
                break;
            case 1:
                defaultSchemes = Arrays.asList(CardType.VISA.name(), CardType.MASTERCARD.name());
                alternativeSchemes = Collections.singletonList(CardType.BANCOMAT.name());
                break;
            case 2:
                // empty default schemes disable the feature
                defaultSchemes = null;
                alternativeSchemes = null;
                break;
            case 3:
                defaultSchemes = Arrays.asList(CardType.VISA.name(), CardType.MASTERCARD.name());
                alternativeSchemes = null;
                break;
            case 4:
                defaultSchemes = Collections.singletonList(CardType.BANKAXEPT.name());
                alternativeSchemes = Arrays.asList(CardType.VISA.name(), CardType.MASTERCARD.name());
                break;
            case 5:
                defaultSchemes = Arrays.asList(CardType.VISA.name(), CardType.MASTERCARD.name());
                alternativeSchemes = Collections.singletonList(CardType.BANKAXEPT.name());
                break;
            default:
                defaultSchemes = null;
                alternativeSchemes = null;
                break;
        }

        if (mid == null) {
            infoTextView.setText("MID is not available yet. Business/MID still loading or failed to load.");
            return;
        }
        enableCustomerChoice(defaultSchemes, alternativeSchemes, mid);
    }

    private void enableCustomerChoice(List<String> defaultSchemes, List<String> alternativeSchemes, String mid) {
        if (configurationService == null) {
            showResult("PoyntConfigurationService is not connected yet");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(Constants.Extras.DEFAULT_SCHEMES,
                defaultSchemes == null ? null : new ArrayList<>(defaultSchemes));
        bundle.putStringArrayList(Constants.Extras.ALTERNATIVE_SCHEMES,
                alternativeSchemes == null ? null : new ArrayList<>(alternativeSchemes));
        bundle.putString(Constants.Extras.MID, mid);

        Log.d(TAG, "enableCustomerChoice: default=" + defaultSchemes
                + " alternative=" + alternativeSchemes + " mid=" + mid);

        try {
            configurationService.enableConsumerChoiceAPI(bundle, new IPoyntConfigurationUpdateListener.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    showResult("Customer choice enabled\ndefault: " + defaultSchemes
                            + "\nalternative: " + alternativeSchemes
                            + "\nMID: " + mid);
                }

                @Override
                public void onFailure() throws RemoteException {
                    showResult("Failed to enable customer choice for MID: " + mid);
                }
            });
        } catch (RemoteException e) {
            showResult("Exception: " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    private void showResult(String result) {
        Log.d(TAG, result);
        runOnUiThread(() -> infoTextView.setText(result));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "binding to service...");
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_CONFIGURATION_SERVICE),
                configurationServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_BUSINESS_SERVICE),
                businessServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "unbinding from service...");
        unbindService(configurationServiceConnection);
        unbindService(businessServiceConnection);
    }

    private ServiceConnection configurationServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "PoyntConfigurationService is now connected");
            configurationService = IPoyntConfigurationService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "PoyntConfigurationService has unexpectedly disconnected");
            configurationService = null;
        }
    };

    private ServiceConnection businessServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "PoyntBusinessService is now connected");
            businessService = IPoyntBusinessService.Stub.asInterface(service);
            initializeBusinessAndMid();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "PoyntBusinessService has unexpectedly disconnected");
            businessService = null;
        }
    };

}
