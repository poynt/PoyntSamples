package co.poynt.samples.dcatestapp.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;

import co.poynt.os.model.APDUData;
import co.poynt.os.model.ConnectionOptions;
import co.poynt.os.model.Intents;
import co.poynt.os.services.v1.IPoyntCardReaderService;
import co.poynt.os.services.v1.IPoyntConfigurationService;
import co.poynt.samples.dcatestapp.databinding.ActivityMainBinding;
import co.poynt.samples.dcatestapp.ui.adapters.ViewPagerAdapter;
import co.poynt.samples.dcatestapp.utils.IHelper;
import co.poynt.samples.dcatestapp.utils.Utils;
import io.reactivex.disposables.CompositeDisposable;

public class NonPaymentCardReaderActivity extends AppCompatActivity implements IHelper {
    private static final String TAG = NonPaymentCardReaderActivity.class.getName();

    private IPoyntCardReaderService cardReaderService;
    private IPoyntConfigurationService poyntConfigurationService;

    private ActivityMainBinding binding;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter cardReaderFilter = new IntentFilter();
        cardReaderFilter.addAction("poynt.intent.action.CONNECTED");
        cardReaderFilter.addAction("poynt.intent.action.DISCONNECTED");
        cardReaderFilter.addAction("poynt.intent.action.CARD_FOUND");
        cardReaderFilter.addAction("poynt.intent.action.PRESENT_CARD");
        cardReaderFilter.addAction("poynt.intent.action.CARD_NOT_REMOVED");
        cardReaderFilter.addAction("poynt.intent.action.PROCESSING_ERROR_COLLISION");
        cardReaderFilter.addAction("poynt.misc.event.NON_PAYMENT_CARD_ACCESS_MODE");
        registerReceiver(uiEventReceiver, cardReaderFilter);

        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_CARD_READER_SERVICE),
                serviceConnection, BIND_AUTO_CREATE);

        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_CONFIGURATION_SERVICE),
                poyntConfigurationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(uiEventReceiver);
        unbindService(serviceConnection);
        unbindService(poyntConfigurationServiceConnection);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.NONE, "Clear")
                .setOnMenuItemClickListener(item -> {
                    View view = this.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    clearLog();
                    return true;
                }).setIcon(android.R.drawable.ic_menu_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    private void setupViews() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setUserInputEnabled(false);
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                recalculate(position, positionOffset);
                binding.consoleScroller.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Misc");
                    break;
                case 1:
                    tab.setText("Emv");
                    break;
                case 2:
                    tab.setText("Mifare");
                    break;
            }
        }).attach();


        //connection options
        binding.newConnectionOptions.setOnCheckedChangeListener((compoundButton, checked) -> {
            binding.connectionInterfaces.setEnabled(checked);
            binding.interfaceCL.setEnabled(checked);
            binding.interfaceEMV.setEnabled(checked);
            binding.interfaceGSM.setEnabled(checked);
            binding.interfaceSLE.setEnabled(checked);
            binding.interfaceMSR.setEnabled(checked);
        });
    }

    private LinearLayoutManager getLayoutManager() {
        if (binding.viewPager.getChildAt(0) instanceof RecyclerView) {
            return (LinearLayoutManager) ((RecyclerView) binding.viewPager.getChildAt(0)).getLayoutManager();
        }
        return null;
    }

    private void recalculate(int position, float positionOffset) {
        LinearLayoutManager layoutManager = getLayoutManager();
        if (layoutManager != null) {
            View leftView = layoutManager.findViewByPosition(position);
            if (leftView == null) return;
            View rightView = layoutManager.findViewByPosition(position + 1);
            int leftHeight = getMeasuredViewHeightFor(leftView);
            ViewPager2.LayoutParams layoutParams = binding.viewPager.getLayoutParams();
            if (rightView != null) {
                int rightHeight = getMeasuredViewHeightFor(rightView);
                layoutParams.height = leftHeight + (int) ((rightHeight - leftHeight) * positionOffset);
            } else {
                layoutParams.height = leftHeight;
            }
            binding.viewPager.setLayoutParams(layoutParams);
            binding.viewPager.invalidate();
        }
    }

    private int getMeasuredViewHeightFor(View view) {
        int wMeasureSpec = View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY);
        int hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(wMeasureSpec, hMeasureSpec);
        return view.getMeasuredHeight();
    }

    public void clearLog() {
        binding.consoleText.setText("");
        binding.consoleScroller.smoothScrollTo(0, binding.consoleText.getBottom());
    }

    @Override
    public void logReceivedMessage(final String message) {
        Log.d(TAG, "LogReceivedMessage : " + message);
        runOnUiThread(() -> {
            SpannableString spannableMessage = Utils.getColoredString(message);
            binding.consoleText.append(spannableMessage);
            binding.consoleText.append("\n\n");
            binding.consoleScroller.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }

    @Override
    public IPoyntCardReaderService getCardReaderService() {
        return cardReaderService;
    }

    @Override
    public IPoyntConfigurationService getPoyntConfigurationService() {
        return poyntConfigurationService;
    }

    @Override
    public void updateConnectionOptionsInterface(ConnectionOptions connectionOptions) {
        if (!binding.newConnectionOptions.isChecked()) {
            //nothing to do here. new logic is not enabled
            return;
        }

        Log.i(TAG, "Overriding Connection Options enabled interfaces");

        byte enabledInterfaces = ConnectionOptions.INTERFACE_NONE;
        ConnectionOptions.ContactInterfaceType contactInterfaceType = null;

        if (binding.interfaceCL.isChecked()) {
            enabledInterfaces |= ConnectionOptions.INTERFACE_CL;
        }
        if (binding.interfaceMSR.isChecked()) {
            enabledInterfaces |= ConnectionOptions.INTERFACE_MSR;
        }

        if (binding.interfaceEMV.isChecked()) {
            enabledInterfaces |= ConnectionOptions.INTERFACE_CT;
            contactInterfaceType = ConnectionOptions.ContactInterfaceType.EMV;
        } else if (binding.interfaceGSM.isChecked()) {
            enabledInterfaces |= ConnectionOptions.INTERFACE_CT;
            contactInterfaceType = ConnectionOptions.ContactInterfaceType.GSM;
        } else if (binding.interfaceSLE.isChecked()) {
            enabledInterfaces |= ConnectionOptions.INTERFACE_CT;
            contactInterfaceType = ConnectionOptions.ContactInterfaceType.SLE;
        }

        connectionOptions.setEnabledInterfaces(enabledInterfaces);
        connectionOptions.setContactInterface(contactInterfaceType);
    }

    @Override
    public void updateAPDUDataInterface(APDUData apduData) {
        if (!binding.newConnectionOptions.isChecked()) {
            //nothing to do here. new logic is not enabled
            return;
        }

        Log.i(TAG, "Overriding APDU data enabled interfaces");

        byte enabledInterfaces = APDUData.INTERFACE_NONE;
        APDUData.ContactInterfaceType contactInterfaceType = null;

        if (binding.interfaceCL.isChecked()) {
            enabledInterfaces |= APDUData.INTERFACE_CL;
        }

        if (binding.interfaceEMV.isChecked()) {
            enabledInterfaces |= APDUData.INTERFACE_CT;
            contactInterfaceType = APDUData.ContactInterfaceType.EMV;
        } else if (binding.interfaceGSM.isChecked()) {
            enabledInterfaces |= APDUData.INTERFACE_CT;
            contactInterfaceType = APDUData.ContactInterfaceType.GSM;
        } else if (binding.interfaceSLE.isChecked()) {
            enabledInterfaces |= APDUData.INTERFACE_CT;
            contactInterfaceType = APDUData.ContactInterfaceType.SLE;
        }

        apduData.setEnabledInterfaces(enabledInterfaces);
        apduData.setContactInterface(contactInterfaceType);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected ");
            cardReaderService = IPoyntCardReaderService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected ");
        }
    };

    /**
     * Class for interacting with the Configuration Service
     */
    private final ServiceConnection poyntConfigurationServiceConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.e("ConfigurationTest", "IPoyntConfigurationService is now connected");
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            poyntConfigurationService = IPoyntConfigurationService.Stub.asInterface(service);
            logReceivedMessage("Connected to Poynt Configuration Service.");
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e("ConfigurationTest", "IPoyntConfigurationService has unexpectedly disconnected");
            logReceivedMessage("Disconnected from Poynt Configuration Service.");
            poyntConfigurationService = null;
        }
    };

    private final BroadcastReceiver uiEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received broadcasted intent: " + intent.getAction());

            if ("poynt.intent.action.CARD_FOUND".equals(intent.getAction())) {
                logReceivedMessage("CARD_FOUND");
                Log.d(TAG, "CARD_FOUND");
            } else if ("poynt.intent.action.PRESENT_CARD".equals(intent.getAction())) {
                logReceivedMessage("PRESENT_CARD");
                Log.d(TAG, "PRESENT_CARD");
            } else if ("poynt.intent.action.CARD_NOT_REMOVED".equals(intent.getAction())) {
                logReceivedMessage("CARD_NOT_REMOVED");
                Log.d(TAG, "CARD_NOT_REMOVED");
            } else if ("poynt.intent.action.PROCESSING_ERROR_COLLISION".equals(intent.getAction())) {
                logReceivedMessage("PROCESSING_ERROR_COLLISION");
                Log.d(TAG, "PROCESSING_ERROR_COLLISION");
            } else if ("poynt.misc.event.NON_PAYMENT_CARD_ACCESS_MODE".equals(intent.getAction())) {
                if (intent.hasExtra("LOG_DATA")) {
                    byte[] logData = intent.getByteArrayExtra("LOG_DATA");
                    Log.d(TAG, "Log " + Arrays.toString(logData));
                    if (logData.length >= 5) {
                        logReceivedMessage("NON_PAYMENT_CARD_ACCESS_MODE - " + logData[4]);
                        Log.d(TAG, "NON_PAYMENT_CARD_ACCESS_MODE: " + logData[4]);
                    } else {
                        logReceivedMessage("Error NON_PAYMENT_CARD_ACCESS_MODE notification - Insufficient data");
                        Log.e(TAG, "NON_PAYMENT_CARD_ACCESS_MODE notification: Insufficient data");
                    }
                } else {
                    logReceivedMessage("Error NON_PAYMENT_CARD_ACCESS_MODE notification - No data");
                    Log.e(TAG, "NON_PAYMENT_CARD_ACCESS_MODE notification: No data");
                }

            } else {
                logReceivedMessage("****UNHANDLED UI EVENT RECEIVED****");
            }
        }
    };
}