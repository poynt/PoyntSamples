package co.poynt.samples.codesamples;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import co.poynt.os.model.APDUData;
import co.poynt.os.model.ConnectionOptions;
import co.poynt.os.model.ConnectionResult;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntCardInsertListener;
import co.poynt.os.services.v1.IPoyntCardReaderService;
import co.poynt.os.services.v1.IPoyntConfigurationService;
import co.poynt.os.services.v1.IPoyntConfigurationUpdateListener;
import co.poynt.os.services.v1.IPoyntConnectToCardListener;
import co.poynt.os.services.v1.IPoyntDisconnectFromCardListener;
import co.poynt.os.services.v1.IPoyntExchangeAPDUListListener;
import co.poynt.os.services.v1.IPoyntExchangeAPDUListener;
import co.poynt.os.util.ByteUtils;
import co.poynt.samples.codesamples.utils.Util;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("StaticFieldLeak")
public class NonPaymentCardReaderActivity extends Activity {
    private IPoyntCardReaderService cardReaderService;
    private IPoyntConfigurationService poyntConfigurationService;

    private static final String TAG = NonPaymentCardReaderActivity.class.getName();
    private ProgressDialog cardRemovalProgress;

    private UIEventReceiver uiEventReceiver;

    private TextView mDumpTextView;
    private ScrollView mScrollView;
    private EditText apduDataInput, etBingRange;

    Button ctSuccessfulTransaction, ctfFileNotFound, ctExchangeApduTest,
            ctCardRejectionMaster, ctXAPDU, ctPymtTrnDuringDCA,
            clSuccessfulTransaction, clFileNotFoundTest, clExchangeApdu, clCardRejectionMaster,
            clXAPDU, clPymtTrnDuringDCA, isoSuccessfulTransaction,
            isoFileNotFound, isoExchangeApdu, isoCardRejectionMaster, isoPymntTrnDuringDca, isoXAPDU,
            sle401, sle402, sle403, sle404, sle405, sle406, sle407, sle408, sle409, sleXAPDU,
            testMifare, testMifareAfterPowerCycle, isoItalianHealthCards, btnSendBinRange;


    private LinearLayout connectionInterfaces;
    private CheckBox newConnectionOptionLogic;
    private CheckBox interfaceCL, interfaceEMV, interfaceGSM, interfaceSLE, interfaceMSR;

    private ServiceConnection serviceConnection = new ServiceConnection() {
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
    private ServiceConnection poyntConfigurationServiceConnection = new ServiceConnection() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_read);
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setupViews();

        Button clearLogBtn = (Button) findViewById(R.id.clearLog);
        clearLogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLog();
            }
        });

        connectionInterfaces = findViewById(R.id.connectionInterfaces);
        interfaceCL = findViewById(R.id.interfaceCL);
        interfaceEMV = findViewById(R.id.interfaceEMV);
        interfaceGSM = findViewById(R.id.interfaceGSM);
        interfaceSLE = findViewById(R.id.interfaceSLE);
        interfaceMSR = findViewById(R.id.interfaceMSR);

        newConnectionOptionLogic = findViewById(R.id.newConnectionOptions);
        newConnectionOptionLogic.setOnCheckedChangeListener((compoundButton, checked) -> {
            connectionInterfaces.setEnabled(checked);
            interfaceCL.setEnabled(checked);
            interfaceEMV.setEnabled(checked);
            interfaceGSM.setEnabled(checked);
            interfaceSLE.setEnabled(checked);
            interfaceMSR.setEnabled(checked);
        });

        final Button connectToCardBtn = (Button) findViewById(R.id.connectToCard);
        connectToCardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        ConnectionOptions connectionOptions = new ConnectionOptions();
                        connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.EMV);
                        connectionOptions.setTimeout(60);
                        updateConnectionOptionsInterface(connectionOptions);
                        try {
                            cardReaderService.connectToCard(connectionOptions,
                                    new IPoyntConnectToCardListener.Stub() {
                                        @Override
                                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                                            logReceivedMessage(connectionResult.toString());
                                        }

                                        @Override
                                        public void onError(PoyntError poyntError) throws RemoteException {
                                            logReceivedMessage(poyntError.toString());
                                        }
                                    });
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        final Button disconnectFromCardBtn = (Button) findViewById(R.id.disconnectFromCard);
        disconnectFromCardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardRemovalProgress = new ProgressDialog(NonPaymentCardReaderActivity.this);
                cardRemovalProgress.setMessage("waiting for card removal");
                cardRemovalProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                cardRemovalProgress.setIndeterminate(true);
                cardRemovalProgress.show();

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        ConnectionOptions connectionOptions = new ConnectionOptions();
                        connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.EMV);
                        connectionOptions.setTimeout(60);
                        updateConnectionOptionsInterface(connectionOptions);
                        try {
                            logReceivedMessage("Please remove card!");
                            cardReaderService.disconnectFromCard(connectionOptions,
                                    new IPoyntDisconnectFromCardListener.Stub() {

                                        @Override
                                        public void onDisconnect() throws RemoteException {
                                            logReceivedMessage("Disconnected");
                                            cardRemovalProgress.dismiss();
                                        }

                                        @Override
                                        public void onError(PoyntError poyntError) throws RemoteException {
                                            logReceivedMessage("onError: " + poyntError.toString());
                                            cardRemovalProgress.dismiss();
                                        }
                                    });
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        final Button checkIfCardInserted = (Button) findViewById(R.id.checkIfCardInserted);
        checkIfCardInserted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        ConnectionOptions connectionOptions = new ConnectionOptions();
                        connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.GSM);
                        connectionOptions.setTimeout(60);
                        updateConnectionOptionsInterface(connectionOptions);
                        try {
                            cardReaderService.checkIfCardInserted(connectionOptions,
                                    new IPoyntCardInsertListener.Stub() {

                                        @Override
                                        public void onCardFound() throws RemoteException {
                                            logReceivedMessage("Card Found");
                                        }

                                        @Override
                                        public void onCardNotFound() throws RemoteException {
                                            logReceivedMessage("Card Not Found");
                                        }

                                        @Override
                                        public void onError(PoyntError poyntError) throws RemoteException {
                                            logReceivedMessage("onError: " + poyntError.toString());
                                        }
                                    });
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        apduDataInput = (EditText) findViewById(R.id.apduData);
        final Button exchangeAPDU = (Button) findViewById(R.id.exchangeAPDU);
        exchangeAPDU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                APDUData apduData = new APDUData();
                apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
                apduData.setTimeout(60);
                apduData.setCommandAPDU(apduDataInput.getText().toString());
                updateAPDUDataInterface(apduData);
                try {
                    cardReaderService.exchangeAPDU(apduData,
                            new IPoyntExchangeAPDUListener.Stub() {


                                @Override
                                public void onSuccess(String rAPDU) throws RemoteException {
                                    logReceivedMessage("Response APDU: " + rAPDU);
                                }

                                @Override
                                public void onError(PoyntError poyntError) throws RemoteException {
                                    logReceivedMessage("onError: " + poyntError.toString());
                                }
                            });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        final Button abortBtn = (Button) findViewById(R.id.abort);
        abortBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {

                        try {
                            logReceivedMessage("Aborting....");
                            cardReaderService.abort();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        final Button readIMSI = (Button) findViewById(R.id.readIMSI);
        readIMSI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectMasterfile(
                        new IPoyntExchangeAPDUListener.Stub() {
                            @Override
                            public void onSuccess(String rAPDU) throws RemoteException {
                                getSelectMasterfileResponse(new IPoyntExchangeAPDUListener.Stub() {
                                    @Override
                                    public void onSuccess(String rAPDU) throws RemoteException {
                                        logReceivedMessage("Response of Select Masterfile: " + rAPDU);
                                        selectGSMDirectory(new IPoyntExchangeAPDUListener.Stub() {
                                            @Override
                                            public void onSuccess(String rAPDU) throws RemoteException {
                                                logReceivedMessage("Response of Select GSM Directory: " + rAPDU);
                                                selectIMSI(new IPoyntExchangeAPDUListener.Stub() {
                                                    @Override
                                                    public void onSuccess(String rAPDU) throws RemoteException {
                                                        logReceivedMessage("Response of select IMSI: " + rAPDU);
                                                        getSelectIMSIResponse(new IPoyntExchangeAPDUListener.Stub() {
                                                            @Override
                                                            public void onSuccess(String rAPDU) throws RemoteException {
                                                                logReceivedMessage("Response of read IMSI: " + rAPDU);
                                                                readBinaryIMSI(new IPoyntExchangeAPDUListener.Stub() {
                                                                    @Override
                                                                    public void onSuccess(String rAPDU) throws RemoteException {
                                                                        logReceivedMessage("Response of read IMSI: " + rAPDU);
                                                                    }

                                                                    @Override
                                                                    public void onError(PoyntError poyntError) throws RemoteException {
                                                                        logReceivedMessage("Read IMSI failed: " + poyntError.toString());
                                                                    }
                                                                });
                                                            }

                                                            @Override
                                                            public void onError(PoyntError poyntError) throws RemoteException {
                                                                logReceivedMessage("Read IMSI failed: " + poyntError.toString());
                                                            }
                                                        });
                                                    }

                                                    @Override
                                                    public void onError(PoyntError poyntError) throws RemoteException {
                                                        logReceivedMessage("Select IMSI failed: " + poyntError.toString());
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onError(PoyntError poyntError) throws RemoteException {
                                                logReceivedMessage("Select GSM Directory failed: " + poyntError.toString());
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(PoyntError poyntError) throws RemoteException {
                                        logReceivedMessage("Read masterfile select failed: " + poyntError.toString());
                                    }
                                });

                            }

                            @Override
                            public void onError(PoyntError poyntError) throws RemoteException {
                                logReceivedMessage("Select Masterfile failed: " + poyntError.toString());
                            }
                        });
            }
        });
    }

    private void setupViews() {

        ctSuccessfulTransaction = findViewById(R.id.successfulTransactionTest);
        ctfFileNotFound = findViewById(R.id.fileNotFoundTest);
        ctExchangeApduTest = findViewById(R.id.exchangeCTApduList);
        ctCardRejectionMaster = findViewById(R.id.ctCardRejectionMaster);
        ctXAPDU = findViewById(R.id.ctXAPDU);

        ctPymtTrnDuringDCA = findViewById(R.id.pymtTransactionDuringDCA);


        clSuccessfulTransaction = findViewById(R.id.successfulTransactionCLTest);
        clFileNotFoundTest = findViewById(R.id.fileNotFoundCLTest);
        clExchangeApdu = findViewById(R.id.exchangeCLApduList);
        clCardRejectionMaster = findViewById(R.id.clCardRejectionMaster);
        clXAPDU = findViewById(R.id.clXAPDU);
        clPymtTrnDuringDCA = findViewById(R.id.clPymtTransactionDuringDCA);

        isoSuccessfulTransaction = findViewById(R.id.iSOSuccessfulTransactionTest);
        isoFileNotFound = findViewById(R.id.iSOfileNotFoundTest);
        isoExchangeApdu = findViewById(R.id.iSOexchangeApduList);
        isoPymntTrnDuringDca = findViewById(R.id.isoTrnDuringDCA);
        isoCardRejectionMaster = findViewById(R.id.isoCardRejectionMaster);
        isoXAPDU = findViewById(R.id.isoXAPDU);
        isoItalianHealthCards = findViewById(R.id.isoItalianHealthCards);

        sle401 = findViewById(R.id.sle401);
        sle402 = findViewById(R.id.sle402);
        sle403 = findViewById(R.id.sle403);
        sle404 = findViewById(R.id.sle404);
        sle405 = findViewById(R.id.sle405);
        sle406 = findViewById(R.id.sle406);
        sle407 = findViewById(R.id.sle407);
        sle408 = findViewById(R.id.sle408);
        sle409 = findViewById(R.id.sle409);
        sleXAPDU = findViewById(R.id.sleXAPDU);
        testMifare = findViewById(R.id.testMifare);
        testMifareAfterPowerCycle = findViewById(R.id.testMifareAfterPowerCycle);

        etBingRange = findViewById(R.id.etBinRange);
        btnSendBinRange = findViewById(R.id.btnSendBinRange);

        ctSuccessfulTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ctSuccessTransactionTest();
            }
        });
        ctfFileNotFound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ctFilenotfound();
            }
        });
        ctExchangeApduTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ctExchangeApdu();
            }
        });
        ctCardRejectionMaster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ctCardRejectionMaster();
            }
        });
        ctXAPDU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ctXAPDU();
            }
        });

        clSuccessfulTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clSuccessTransaction();
            }
        });
        clFileNotFoundTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clFileNotFound();
            }
        });
        clExchangeApdu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clExchangeApdu();
            }
        });

        clCardRejectionMaster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clCardRejectionMaster();
            }
        });
        clXAPDU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clXAPDU();
            }
        });
        clPymtTrnDuringDCA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clPymtTrnDuringDCA();
            }
        });
        ctPymtTrnDuringDCA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ctPymtTrnDuringDCA();
            }
        });

        isoSuccessfulTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isoSuccessfulTransaction();
            }
        });
        isoFileNotFound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isoFileNotFound();
            }
        });
        isoExchangeApdu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isoExchangeApdu();
            }
        });

        isoPymntTrnDuringDca.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isoPymntTrnDuringDca();
            }
        });

        isoCardRejectionMaster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isoCardRejectionMaster();
            }
        });
        isoXAPDU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isoXAPDU();
            }
        });
        isoItalianHealthCards.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isoItalianHealthCards();
            }
        });

        sle401.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sle401();
            }
        });
        sle402.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sle402();
            }
        });
        sle403.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sle403();
            }
        });
        sle404.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sle404();
            }
        });
        sle405.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sle405();
            }
        });
        sle406.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sle406();
            }
        });
        sle407.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sle407();
            }
        });
        sle408.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sle408();
            }
        });
        sle409.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sle409();
            }
        });
        sleXAPDU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sleXAPDU();
            }
        });

        testMifare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMifareTest();
            }
        });

        testMifareAfterPowerCycle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMifareTestAfterPowerCycle();
            }
        });

        btnSendBinRange.setOnClickListener(view -> setBinRange());
    }


    protected void onResume() {
        super.onResume();
        IntentFilter cardReaderFilter = new IntentFilter("poynt.intent.action.CONNECTED");
        cardReaderFilter.addAction("poynt.intent.action.DISCONNECTED");
        cardReaderFilter.addAction("poynt.intent.action.CARD_FOUND");
        cardReaderFilter.addAction("poynt.intent.action.PRESENT_CARD");
        cardReaderFilter.addAction("poynt.intent.action.CARD_NOT_REMOVED");
        cardReaderFilter.addAction("poynt.intent.action.PROCESSING_ERROR_COLLISION");
        cardReaderFilter.addAction("poynt.misc.event.NON_PAYMENT_CARD_ACCESS_MODE");

        uiEventReceiver = new UIEventReceiver();
        registerReceiver(uiEventReceiver, cardReaderFilter);

        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_CARD_READER_SERVICE),
                serviceConnection, BIND_AUTO_CREATE);

        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_CONFIGURATION_SERVICE),
                poyntConfigurationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    protected void onPause() {
        super.onPause();
        // always change the mode

        unregisterReceiver(uiEventReceiver);
        unbindService(serviceConnection);
        unbindService(poyntConfigurationServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_order, menu);
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

    private class UIEventReceiver extends BroadcastReceiver {
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
    }

    public void logReceivedMessage(final String message) {
        Log.d(TAG, "LogReceivedMessage : " + message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDumpTextView.append(message + "\n\n");
                mScrollView.fullScroll(ScrollView.FOCUS_UP);

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

    private void selectMasterfile(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = new APDUData();
        apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
        apduData.setTimeout(60);
        apduData.setCommandAPDU("04A0A40000023F0000");
        updateAPDUDataInterface(apduData);
        try {
            cardReaderService.exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void getSelectMasterfileResponse(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = new APDUData();
        apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
        apduData.setTimeout(60);
        apduData.setCommandAPDU("02A0C0000000");
        updateAPDUDataInterface(apduData);
        try {
            cardReaderService.exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void selectGSMDirectory(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = new APDUData();
        apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
        apduData.setTimeout(60);
        apduData.setCommandAPDU("04A0A40000027F2000");
        updateAPDUDataInterface(apduData);
        try {
            cardReaderService.exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void selectIMSI(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = new APDUData();
        apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
        apduData.setTimeout(60);
        apduData.setCommandAPDU("04A0A40000026F0700");
        updateAPDUDataInterface(apduData);
        try {
            cardReaderService.exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }


    private void getSelectIMSIResponse(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = new APDUData();
        apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
        apduData.setTimeout(60);
        apduData.setCommandAPDU("02A0C0000000");
        updateAPDUDataInterface(apduData);
        try {
            cardReaderService.exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void readBinaryIMSI(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = new APDUData();
        apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
        apduData.setTimeout(60);
        apduData.setCommandAPDU("02A0B0000009");
        updateAPDUDataInterface(apduData);
        try {
            cardReaderService.exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    // Test cases

    private void ctSuccessTransactionTest() {
        logReceivedMessage("===============================");
        logReceivedMessage("Successful non-payment transaction Test");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.EMV);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("checkIfCardInserted : ConnectionOptions : " + connectionOptions);
            cardReaderService.checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {

                    logReceivedMessage("CardFound");
                    logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
                    cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success : connectionResult " + connectionResult);
                            APDUData apduData = new APDUData();
                            apduData.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
                            apduData.setContactInterface(APDUData.ContactInterfaceType.EMV);
                            apduData.setTimeout(30);
                            updateAPDUDataInterface(apduData);
                            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                            cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    logReceivedMessage("Exchange APDU result : " + s);

                                    if (s.endsWith("9000")) {
                                        logReceivedMessage("Successful non-payment transaction Test Passed");
                                    } else {
                                        logReceivedMessage("Successful non-payment transaction Test Failed");
                                    }
                                    disconnectCardReader(connectionOptions);
                                }

                                @Override
                                public void onError(PoyntError poyntError) throws RemoteException {
                                    logReceivedMessage("APDU exchange failed " + poyntError);
                                    disconnectCardReader(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card Not Found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void disconnectCardReader(ConnectionOptions connectionOptions) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cardRemovalProgress = new ProgressDialog(NonPaymentCardReaderActivity.this);
                    cardRemovalProgress.setMessage("waiting for card removal");
                    cardRemovalProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    cardRemovalProgress.setIndeterminate(true);
                    cardRemovalProgress.show();
                }
            });

            logReceivedMessage("disconnectFromCard : connectionOptions" + connectionOptions);
            cardReaderService.disconnectFromCard(connectionOptions, new IPoyntDisconnectFromCardListener.Stub() {
                @Override
                public void onDisconnect() throws RemoteException {
                    logReceivedMessage("Disconnected");
                    cardRemovalProgress.dismiss();
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Disconnection failed " + poyntError.toString());
                    cardRemovalProgress.dismiss();
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // CL test cases

    private void ctFilenotfound() {
        logReceivedMessage("===============================");
        logReceivedMessage("File Not Found Test");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.EMV);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            cardReaderService.checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");
                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
                    cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success : connectionResult " + connectionResult);
                            APDUData apduData = new APDUData();
                            apduData.setCommandAPDU("0400A404000A4F53452E5641532E303100");
                            apduData.setContactInterface(APDUData.ContactInterfaceType.EMV);
                            apduData.setTimeout(30);
                            updateAPDUDataInterface(apduData);
                            logReceivedMessage("exchangeAPDU apduData " + apduData);
                            cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    logReceivedMessage("Exchange APDU result : " + s);

                                    if (s.endsWith("6A82")) {
                                        logReceivedMessage("File not found Test Passed");
                                    } else {
                                        logReceivedMessage("APDU response " + s);
                                    }
                                    disconnectCardReader(connectionOptions);
                                }

                                @Override
                                public void onError(PoyntError poyntError) throws RemoteException {
                                    logReceivedMessage("APDU exchange failed " + poyntError);
                                    disconnectCardReader(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card Not Found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    private void ctExchangeApdu() {
        logReceivedMessage("===============================");
        logReceivedMessage("Exchange CT APDU Test");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.EMV);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            cardReaderService.checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");
                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
                    cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success : connectionResult " + connectionResult);
                            APDUData apduData1 = new APDUData();
                            apduData1.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
                            apduData1.setContactInterface(APDUData.ContactInterfaceType.EMV);
                            apduData1.setTimeout(30);
                            updateAPDUDataInterface(apduData1);

                            APDUData apduData2 = new APDUData();
                            apduData2.setCommandAPDU("0400A404000E325041592E5359532E444446303100");
                            apduData2.setOkCondition("6A816A82");
                            apduData2.setContactInterface(APDUData.ContactInterfaceType.EMV);
                            apduData2.setTimeout(30);
                            updateAPDUDataInterface(apduData2);

                            APDUData apduData3 = new APDUData();
                            apduData3.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
                            apduData3.setContactInterface(APDUData.ContactInterfaceType.EMV);
                            apduData3.setTimeout(30);
                            updateAPDUDataInterface(apduData3);

                            logReceivedMessage("exchangeAPDUList : apduData " + Arrays.asList(apduData1, apduData2, apduData3));

                            cardReaderService.exchangeAPDUList(Arrays.asList(apduData1, apduData2, apduData3), new IPoyntExchangeAPDUListListener.Stub() {
                                @Override
                                public void onResult(List<String> list, PoyntError poyntError) throws RemoteException {
                                    if (poyntError != null) {
                                        logReceivedMessage("Exchange APDU List failed " + poyntError);
                                    } else {
                                        logReceivedMessage("Exchange APDU result : " + list.toString());
                                        if (list.size() >= 3) {
                                            if (list.get(0).endsWith("9000") && list.get(2).endsWith("9000")) {
                                                if (list.get(1).endsWith("9000") || list.get(1).endsWith("6A82")) {
                                                    logReceivedMessage("Exchange APDU list test passed");

                                                }
                                            }
                                        }
                                    }
                                    disconnectCardReader(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card not found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    // CL Tests
    private void clSuccessTransaction() {
        logReceivedMessage("===============================");
        logReceivedMessage("Successful non-payment transaction CL Test");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);
                    APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("0400A404000E325041592E5359532E444446303100");
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apuduData  " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("Successful non-payment CL transaction Test Passed");
                            } else {
                                logReceivedMessage("Successful non-payment CL transaction Test Failed");
                            }
                            disconnectCardReader(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    private void clFileNotFound() {
        logReceivedMessage("===============================");
        logReceivedMessage("File Not Found CL Test");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : connectionOptions : " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);
                    APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("0400A404000A4F53452E5641532E303100");
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU apduData " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("6A82")) {
                                logReceivedMessage("File not found CL Test Passed");
                            } else {
                                logReceivedMessage("APDU response " + s);
                            }
                            disconnectCardReader(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    private void clExchangeApdu() {
        logReceivedMessage("===============================");
        logReceivedMessage("Exchange CL APDU Test");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);
                    APDUData apduData1 = new APDUData();
                    apduData1.setCommandAPDU("0400A404000E325041592E5359532E444446303100");
                    apduData1.setTimeout(30);
                    updateAPDUDataInterface(apduData1);

                    APDUData apduData2 = new APDUData();
                    apduData2.setCommandAPDU("0400A404000A4F53452E5641532E303100");
                    apduData2.setTimeout(30);
                    updateAPDUDataInterface(apduData2);

                    apduData2.setOkCondition("6A82");
                    APDUData apduData3 = new APDUData();
                    apduData3.setCommandAPDU("0400A404000E325041592E5359532E444446303100");
                    apduData3.setTimeout(30);
                    updateAPDUDataInterface(apduData3);

                    logReceivedMessage("exchangeAPDUList : " + Arrays.asList(apduData1, apduData2, apduData3));
                    cardReaderService.exchangeAPDUList(Arrays.asList(apduData1, apduData2, apduData3), new IPoyntExchangeAPDUListListener.Stub() {
                        @Override
                        public void onResult(List<String> list, PoyntError poyntError) throws RemoteException {
                            if (poyntError != null) {
                                logReceivedMessage("Exchange CL APDU List failed " + poyntError);
                            } else {
                                logReceivedMessage("Exchange APDU result : " + list.toString());
                                if (list.size() >= 2) {
                                    if (list.get(1).endsWith("6A82")) {
                                        logReceivedMessage("Exchange APDU list Test passed");
                                    }
                                }
                            }
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void ctCardRejectionMaster() {
        logReceivedMessage("===============================");
        logReceivedMessage("CT Payment Card Rejection Test - Master Card");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.EMV);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);
                    final APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
                    apduData.setContactInterface(APDUData.ContactInterfaceType.EMV);
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apdudData " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);
                            if (s.endsWith("9000")) {
                                APDUData apduData2 = new APDUData();
                                apduData2.setCommandAPDU("0400A4040007A000000003101000");
                                apduData2.setContactInterface(APDUData.ContactInterfaceType.EMV);
                                apduData2.setTimeout(30);
                                updateAPDUDataInterface(apduData2);
                                logReceivedMessage("exchangeAPDU : apdudData " + apduData2);
                                cardReaderService.exchangeAPDU(apduData2, new IPoyntExchangeAPDUListener.Stub() {
                                    @Override
                                    public void onSuccess(String s) throws RemoteException {
                                        logReceivedMessage("Exchange APDU result : " + s);
                                        disconnectCardReader(connectionOptions);
                                    }

                                    @Override
                                    public void onError(PoyntError poyntError) throws RemoteException {
                                        logReceivedMessage("APDU exchange failed " + poyntError);
                                        logReceivedMessage("PaymentCard Rejection Success");
                                        disconnectCardReader(connectionOptions);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    private void ctXAPDU() {
        logReceivedMessage("===============================");
        logReceivedMessage("CT XAPDU");
        try {
            logReceivedMessage("Exchange APDU");
            APDUData apduData = new APDUData();
            apduData.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
            apduData.setContactInterface(APDUData.ContactInterfaceType.EMV);
            apduData.setTimeout(30);
            updateAPDUDataInterface(apduData);
            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
            cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    logReceivedMessage("Exchange APDU result : " + s);
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("APDU exchange failed " + poyntError);
                    logReceivedMessage("Test passed");
                    final ConnectionOptions connectionOptions = new ConnectionOptions();
                    connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.EMV);
                    connectionOptions.setTimeout(30);
                    updateConnectionOptionsInterface(connectionOptions);
                    logReceivedMessage("disconnectCardReader : ConnectionOptions : " + connectionOptions);
                    disconnectCardReader(connectionOptions);
                }
            });
        } catch (Exception e) {
            logReceivedMessage(e.getMessage());
        }
    }

    private void clCardRejectionMaster() {
        logReceivedMessage("===============================");
        logReceivedMessage("CL Payment Card Rejection Test - Master Card");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);
                    final APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("0400A404000E325041592E5359532E444446303100");
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apuduData " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);
                            if (s.endsWith("9000")) {
                                APDUData apduData2 = new APDUData();
                                apduData2.setCommandAPDU("0400A4040007A000000003101000");
                                apduData2.setTimeout(30);
                                updateAPDUDataInterface(apduData2);
                                logReceivedMessage("exchangeAPDU : apduData " + apduData2);
                                cardReaderService.exchangeAPDU(apduData2, new IPoyntExchangeAPDUListener.Stub() {
                                    @Override
                                    public void onSuccess(String s) throws RemoteException {
                                        logReceivedMessage("Exchange APDU result : " + s);
                                        disconnectCardReader(connectionOptions);
                                    }

                                    @Override
                                    public void onError(PoyntError poyntError) throws RemoteException {
                                        logReceivedMessage("APDU exchange failed " + poyntError);
                                        logReceivedMessage("PaymentCard Rejection Success");
                                        disconnectCardReader(connectionOptions);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void clXAPDU() {
        logReceivedMessage("===============================");
        logReceivedMessage("CL XAPDU");
        try {
            logReceivedMessage("Exchange APDU");
            APDUData apduData = new APDUData();
            apduData.setCommandAPDU("0400A404000E325041592E5359532E444446303100");
            apduData.setTimeout(30);
            updateAPDUDataInterface(apduData);
            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
            cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    logReceivedMessage("Exchange APDU result : " + s);
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("APDU exchange failed " + poyntError);
                    logReceivedMessage("Test passed");
                    final ConnectionOptions connectionOptions = new ConnectionOptions();
                    connectionOptions.setTimeout(30);
                    updateConnectionOptionsInterface(connectionOptions);
                    logReceivedMessage("disconnectCardReader : ConnectionOptions : " + connectionOptions);
                    disconnectCardReader(connectionOptions);
                }
            });
        }catch (Exception e){
            logReceivedMessage(e.getMessage());
        }
    }

    private void clPymtTrnDuringDCA() {
        logReceivedMessage("===============================");
        logReceivedMessage("204b Payment Transaction during DCA (PoyntC Only)");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);

                    final APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("0400A404000E325041592E5359532E444446303100");
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apduData " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);
                            if (s.endsWith("9000")) {
                                logReceivedMessage("Now open the Terminal app and perform a Payment transaction, Transaction should be success");
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void ctPymtTrnDuringDCA() {
        logReceivedMessage("===============================");
        logReceivedMessage("104b Payment Transaction during DCA (PoyntC Only)");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.EMV);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);

                    final APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
                    apduData.setContactInterface(APDUData.ContactInterfaceType.EMV);
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apduData " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);
                            if (s.endsWith("9000")) {
                                logReceivedMessage("Now open the Terminal app and perform a Payment transaction, Transaction should be success");
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    // ISO 7816 tests

    private void isoSuccessfulTransaction() {
        logReceivedMessage("===============================");
        logReceivedMessage("Successful non-payment transaction Test");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.GSM);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("checkIfCardInserted : ConnectionOptions : " + connectionOptions);
            cardReaderService.checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {

                    logReceivedMessage("CardFound");
                    logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
                    cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                            APDUData apduData = new APDUData();
                            apduData.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
                            apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
                            apduData.setTimeout(30);
                            updateAPDUDataInterface(apduData);
                            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                            cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    logReceivedMessage("Exchange APDU result : " + s);

                                    if (s.endsWith("9000") || s.endsWith("6A81") || s.endsWith("6A82")) {
                                        logReceivedMessage("Successful non-payment transaction Test Passed");
                                    } else {
                                        logReceivedMessage("Successful non-payment transaction Test Failed");
                                    }
                                    disconnectCardReader(connectionOptions);
                                }

                                @Override
                                public void onError(PoyntError poyntError) throws RemoteException {
                                    logReceivedMessage("APDU exchange failed " + poyntError);
                                    disconnectCardReader(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card Not Found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoFileNotFound() {
        logReceivedMessage("===============================");
        logReceivedMessage("File Not Found Test");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.GSM);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            cardReaderService.checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");
                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
                    cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                            APDUData apduData = new APDUData();
                            apduData.setCommandAPDU("0400A404000A4F53452E5641532E303100");
                            apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
                            apduData.setTimeout(30);
                            updateAPDUDataInterface(apduData);
                            logReceivedMessage("exchangeAPDU apduData " + apduData);
                            cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    logReceivedMessage("Exchange APDU result : " + s);

                                    if (s.endsWith("6A82")) {
                                        logReceivedMessage("File not found Test Passed");
                                    } else {
                                        logReceivedMessage("APDU response " + s);
                                    }
                                    disconnectCardReader(connectionOptions);
                                }

                                @Override
                                public void onError(PoyntError poyntError) throws RemoteException {
                                    logReceivedMessage("APDU exchange failed " + poyntError);
                                    disconnectCardReader(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card Not Found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoExchangeApdu() {
        logReceivedMessage("===============================");
        logReceivedMessage("Exchange ISO APDU Test");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.GSM);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            cardReaderService.checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");
                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
                    cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                            APDUData apduData1 = new APDUData();
                            apduData1.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
                            apduData1.setOkCondition("6A816A82");
                            apduData1.setContactInterface(APDUData.ContactInterfaceType.GSM);
                            apduData1.setTimeout(30);
                            updateAPDUDataInterface(apduData1);

                            APDUData apduData2 = new APDUData();
                            apduData2.setCommandAPDU("0400A404000A4F53452E5641532E303100");
                            apduData2.setOkCondition("6A816A82");
                            apduData2.setContactInterface(APDUData.ContactInterfaceType.GSM);
                            apduData2.setTimeout(30);
                            updateAPDUDataInterface(apduData2);

                            APDUData apduData3 = new APDUData();
                            apduData3.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
                            apduData3.setOkCondition("6A816A82");
                            apduData3.setContactInterface(APDUData.ContactInterfaceType.GSM);
                            apduData3.setTimeout(30);
                            updateAPDUDataInterface(apduData3);

                            logReceivedMessage("exchangeAPDUList : apduData " + Arrays.asList(apduData1, apduData2, apduData3));

                            cardReaderService.exchangeAPDUList(Arrays.asList(apduData1, apduData2, apduData3), new IPoyntExchangeAPDUListListener.Stub() {
                                @Override
                                public void onResult(List<String> list, PoyntError poyntError) throws RemoteException {
                                    if (poyntError != null) {
                                        logReceivedMessage("Exchange APDU List failed " + poyntError);
                                    } else {
                                        logReceivedMessage("Exchange APDU result : " + list.toString());
                                        if (list.size() >= 3) {
                                            if (list.get(0).endsWith("9000") || list.get(0).endsWith("6A81") || list.get(0).endsWith("6A82")) {
                                                if (list.get(1).endsWith("9000") || list.get(1).endsWith("6A81") || list.get(1).endsWith("6A82")) {
                                                    if (list.get(2).endsWith("9000") || list.get(2).endsWith("6A81") || list.get(2).endsWith("6A82")) {
                                                        logReceivedMessage("Exchange APDU list test passed");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    disconnectCardReader(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card not found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoPymntTrnDuringDca() {
        logReceivedMessage("===============================");
        logReceivedMessage("ISO Payment transaction during DCA Test");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.GSM);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            cardReaderService.checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");
                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
                    cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                            APDUData apduData = new APDUData();
                            apduData.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
                            apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
                            apduData.setTimeout(30);
                            updateAPDUDataInterface(apduData);
                            logReceivedMessage("exchangeAPDU apduData " + apduData);
                            cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    logReceivedMessage("Exchange APDU result : " + s);

                                    if (s.endsWith("6A81") || s.endsWith("6A82") || s.endsWith("9000")) {
                                        logReceivedMessage("Exchange APDU success");
                                        logReceivedMessage("Now open the Terminal app and perform a Payment transaction, Transaction should be success");
                                    } else {
                                        logReceivedMessage("APDU response " + s);
                                    }
                                }

                                @Override
                                public void onError(PoyntError poyntError) throws RemoteException {
                                    logReceivedMessage("APDU exchange failed " + poyntError);
                                    disconnectCardReader(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card Not Found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoCardRejectionMaster() {
        logReceivedMessage("===============================");
        logReceivedMessage("ISO Payment Card Rejection Test - Master Card");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setTimeout(30);
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.GSM);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);
                    final APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
                    apduData.setTimeout(30);
                    apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apduData " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);
                            if (s.endsWith("9000")) {
                                APDUData apduData2 = new APDUData();
                                apduData2.setCommandAPDU("0400A4040007A000000004101000");
                                apduData2.setTimeout(30);
                                apduData2.setContactInterface(APDUData.ContactInterfaceType.GSM);
                                updateAPDUDataInterface(apduData);
                                logReceivedMessage("exchangeAPDU : apuduData " + apduData2);
                                cardReaderService.exchangeAPDU(apduData2, new IPoyntExchangeAPDUListener.Stub() {
                                    @Override
                                    public void onSuccess(String s) throws RemoteException {
                                        logReceivedMessage("Exchange APDU result : " + s);
                                        disconnectCardReader(connectionOptions);
                                    }

                                    @Override
                                    public void onError(PoyntError poyntError) throws RemoteException {
                                        logReceivedMessage("APDU exchange failed " + poyntError);
                                        logReceivedMessage("PaymentCard Rejection Success");
                                        disconnectCardReader(connectionOptions);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoXAPDU(){
        logReceivedMessage("===============================");
        logReceivedMessage("iso XAPDU");
        try {
            logReceivedMessage("Exchange APDU");
            APDUData apduData = new APDUData();
            apduData.setCommandAPDU("0400A404000E315041592E5359532E444446303100");
            apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
            apduData.setTimeout(30);
            updateAPDUDataInterface(apduData);
            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
            cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    logReceivedMessage("Exchange APDU result : " + s);
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("APDU exchange failed " + poyntError);
                    logReceivedMessage("Test passed");
                    final ConnectionOptions connectionOptions = new ConnectionOptions();
                    connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.GSM);
                    connectionOptions.setTimeout(30);
                    updateConnectionOptionsInterface(connectionOptions);
                    logReceivedMessage("disconnectCardReader : ConnectionOptions : " + connectionOptions);
                    disconnectCardReader(connectionOptions);
                }
            });
        }catch (Exception e){
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoItalianHealthCards() {
        logReceivedMessage("===============================");
        logReceivedMessage("ISO Italian Health Card Test");

        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.GSM);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            cardReaderService.checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");

                    APDUData apduData1 = new APDUData();
                    apduData1.setCommandAPDU("0400A40000023F0000");
                    apduData1.setContactInterface(APDUData.ContactInterfaceType.GSM);
                    apduData1.setTimeout(30);
                    updateAPDUDataInterface(apduData1);

                    APDUData apduData2 = new APDUData();
                    apduData2.setCommandAPDU("0400A4000002110000");
                    apduData2.setContactInterface(APDUData.ContactInterfaceType.GSM);
                    apduData2.setTimeout(30);
                    updateAPDUDataInterface(apduData2);

                    APDUData apduData3 = new APDUData();
                    apduData3.setCommandAPDU("0400A4000002110200");
                    apduData3.setContactInterface(APDUData.ContactInterfaceType.GSM);
                    apduData3.setTimeout(30);
                    updateAPDUDataInterface(apduData3);

                    APDUData apduData4 = new APDUData();
                    apduData4.setCommandAPDU("0300B0000000");
                    apduData4.setContactInterface(APDUData.ContactInterfaceType.GSM);
                    apduData4.setTimeout(30);
                    updateAPDUDataInterface(apduData4);

                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);

                    isoConnectToCardObservable(connectionOptions).flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> {
                        return exchangeAPDUListObservable(generateISOApduList(apduData1, "Select MF"), "Select MF", false);
                    }).takeWhile(list -> {
                        if (list.size() > 0) {
                            logReceivedMessage("APDU exchange Success For :" + apduData1);
                            return true;
                        } else {
                            logReceivedMessage("APDU exchange Failed For : " + apduData1);
                            return false;
                        }
                    }).flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                            generateISOApduList(apduData2, "Select DF1"), "Select DF1", false))
                            .takeWhile(list -> {
                                if (list.size() > 0) {
                                    logReceivedMessage("APDU exchange Success For : " + apduData2);
                                    return true;
                                } else {
                                    logReceivedMessage("APDU exchange Failed For : " + apduData2);
                                    return false;
                                }
                            })
                            .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                                    generateISOApduList(apduData3, "Select EF"), "Select EF", false))
                            .takeWhile(list -> {
                                if (list.size() > 0) {
                                    logReceivedMessage("APDU exchange Success For : " + apduData3);
                                    return true;
                                } else {
                                    logReceivedMessage("APDU exchange Failed For : " + apduData3);
                                    return false;
                                }
                            })
                            .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                                    generateISOApduList(apduData4, "Read EF"), "Read EF", false))
                            .takeWhile(list -> {
                                if (list.size() > 0) {
                                    logReceivedMessage("APDU exchange Success For : " + apduData4);
                                    return true;
                                } else {
                                    logReceivedMessage("APDU exchange Failed  For : " + apduData4);
                                    return false;
                                }
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(new Observer<List<String>>() {
                                @Override
                                public void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                public void onNext(@NonNull List<String> list) {
                                    logReceivedMessage("Done with the test");
                                    disconnectCardReader(connectionOptions);
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    disconnectCardReader(connectionOptions);
                                }

                                @Override
                                public void onComplete() {
                                    disconnectCardReader(connectionOptions);
                                }
                            });

                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card not found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }
    // ================= SLE 4442 ========================

    private void sle401() {
        logReceivedMessage("===============================");
        logReceivedMessage("401 Test for Read Memory");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.SLE);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("03A00100000400800008");
                    apduData.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("401 Test for Read Memory  Test Passed");
                            } else {
                                logReceivedMessage("401 Test for Read Memory Test Failed");
                            }
                            disconnectCardReader(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    private void sle402() {
        logReceivedMessage("===============================");
        logReceivedMessage("402 Read with Protect Bit");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.SLE);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("03A00400000400100004");
                    apduData.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("402 Read with Protect Bit Test Passed");
                            } else {
                                logReceivedMessage("402 Read with Protect Bit Test Failed");
                            }
                            disconnectCardReader(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    private void sle403() {
        logReceivedMessage("===============================");
        logReceivedMessage("403 Verify Programmable Security Code (PSC)");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.SLE);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("03A003000003FFFFFF");
                    apduData.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("403 Verify Programmable Security Code (PSC) Test Passed");
                            } else {
                                logReceivedMessage("403 Verify Programmable Security Code (PSC) Test Failed");
                            }
                            disconnectCardReader(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    private void sle404() {
        logReceivedMessage("===============================");
        logReceivedMessage("404 Change Memory");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.SLE);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);

                    APDUData apduData1 = new APDUData();
                    apduData1.setCommandAPDU("03A003000003FFFFFF");
                    apduData1.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData1.setTimeout(30);
                    updateAPDUDataInterface(apduData1);

                    APDUData apduData2 = new APDUData();
                    apduData2.setCommandAPDU("03A00100000400800008");
                    apduData2.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData2.setTimeout(30);
                    updateAPDUDataInterface(apduData2);

                    APDUData apduData3 = new APDUData();
                    apduData3.setCommandAPDU("03A002000003008055");
                    apduData3.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData3.setTimeout(30);
                    updateAPDUDataInterface(apduData3);

                    APDUData apduData4 = new APDUData();
                    apduData4.setCommandAPDU("03A00100000400800008");
                    apduData4.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData4.setTimeout(30);
                    updateAPDUDataInterface(apduData4);

                    APDUData apduData5 = new APDUData();
                    apduData5.setCommandAPDU("03A002000003008022");
                    apduData5.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData5.setTimeout(30);
                    updateAPDUDataInterface(apduData5);

                    APDUData apduData6 = new APDUData();
                    apduData6.setCommandAPDU("03A00100000400800008");
                    apduData6.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData6.setTimeout(30);
                    updateAPDUDataInterface(apduData6);

                    logReceivedMessage("exchangeAPDUList : apduData " + Arrays.asList(apduData1, apduData2, apduData3, apduData4, apduData5, apduData6));

                    cardReaderService.exchangeAPDUList(Arrays.asList(apduData1, apduData2, apduData3, apduData4, apduData5, apduData6), new IPoyntExchangeAPDUListListener.Stub() {
                        @Override
                        public void onResult(List<String> list, PoyntError poyntError) throws RemoteException {
                            if (poyntError != null) {
                                logReceivedMessage("Exchange APDU List failed " + poyntError);
                            } else {
                                logReceivedMessage("Exchange APDU result : " + list.toString());
                                if (list.size() >= 3) {
                                    if (list.get(0).endsWith("9000") && list.get(1).endsWith("9000") &&
                                            list.get(2).endsWith("9000") && list.get(3).endsWith("9000") &&
                                            list.get(4).endsWith("9000") && list.get(5).endsWith("9000")) {
                                        if (list.get(3).startsWith("55") && list.get(5).startsWith("22")) {
                                            logReceivedMessage("404 Change Memory test passed");
                                        }
                                    }
                                }
                            }
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void sle405() {
        logReceivedMessage("===============================");
        logReceivedMessage("405 Change Memory w/ Protect Bit");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.SLE);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);

                    APDUData apduData1 = new APDUData();
                    apduData1.setCommandAPDU("03A003000003FFFFFF");
                    apduData1.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData1.setTimeout(30);
                    updateAPDUDataInterface(apduData1);

                    APDUData apduData2 = new APDUData();
                    apduData2.setCommandAPDU("03A00400000400100004");
                    apduData2.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData2.setTimeout(30);
                    updateAPDUDataInterface(apduData2);

                    APDUData apduData3 = new APDUData();
                    apduData3.setCommandAPDU("03A0050000030010AB");
                    apduData3.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData3.setTimeout(30);
                    updateAPDUDataInterface(apduData3);

                    APDUData apduData4 = new APDUData();
                    apduData4.setCommandAPDU("03A00400000400100004");
                    apduData4.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData4.setTimeout(30);
                    updateAPDUDataInterface(apduData4);


                    logReceivedMessage("exchangeAPDUList : apduData " + Arrays.asList(apduData1, apduData2, apduData3, apduData4));

                    cardReaderService.exchangeAPDUList(Arrays.asList(apduData1, apduData2, apduData3, apduData4), new IPoyntExchangeAPDUListListener.Stub() {
                        @Override
                        public void onResult(List<String> list, PoyntError poyntError) throws RemoteException {
                            if (poyntError != null) {
                                logReceivedMessage("Exchange APDU List failed " + poyntError);
                            } else {
                                logReceivedMessage("Exchange APDU result : " + list.toString());
                                if (list.size() >= 3) {
                                    if (list.get(0).endsWith("9000") && list.get(1).endsWith("9000") &&
                                            list.get(2).endsWith("9000") && list.get(3).endsWith("9000")) {
                                        if (list.get(3).startsWith("AB")) {
                                            logReceivedMessage("405 Change Memory w/ Protect Bit test passed");
                                        }
                                    }
                                }
                            }
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void sle406() {
        logReceivedMessage("===============================");
        logReceivedMessage("406 Raw Mode ");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.SLE);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("03A080000003C1C2C3");
                    apduData.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("406 Raw Mode Test Passed");
                            } else {
                                logReceivedMessage("406 Raw Mode Test Failed");
                            }
                            disconnectCardReader(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void sle407() {
        logReceivedMessage("===============================");
        logReceivedMessage("407 Raw Mode with Response ");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.SLE);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("03A0810000053040000008");
                    apduData.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("407 Raw Mode with Response Test passed");
                            } else {
                                logReceivedMessage("407 Raw Mode with Response Test failed");
                            }
                            disconnectCardReader(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void sle408() {
        logReceivedMessage("===============================");
        logReceivedMessage("408 Payment Transaction during DCA (PoyntC Only)");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.SLE);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("03A00100000400800008");
                    apduData.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("Perform a payment transaction");
                            } else {
                                logReceivedMessage("408 Payment Transaction during DCA Test failed");
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void sle409() {
        logReceivedMessage("===============================");
        logReceivedMessage("409 Test Use of the Abort command ");
        try {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.SLE);
            connectionOptions.setTimeout(30);
            updateConnectionOptionsInterface(connectionOptions);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = new APDUData();
                    apduData.setCommandAPDU("03A00100000400800008");
                    apduData.setContactInterface(APDUData.ContactInterfaceType.SLE);
                    apduData.setTimeout(30);
                    updateAPDUDataInterface(apduData);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("Send Abort Command");
                            } else {
                                logReceivedMessage("410 Test Use of the Abort command");
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectCardReader(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }
    private void sleXAPDU(){
        logReceivedMessage("===============================");
        logReceivedMessage("SLE XAPDU");
        try {
            logReceivedMessage("Exchange APDU");
            APDUData apduData = new APDUData();
            apduData.setCommandAPDU("03A00100000400800008");
            apduData.setTimeout(30);
            apduData.setContactInterface(APDUData.ContactInterfaceType.SLE);
            updateAPDUDataInterface(apduData);
            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
            cardReaderService.exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    logReceivedMessage("Exchange APDU result : " + s);
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("APDU exchange failed " + poyntError);
                    logReceivedMessage("Test passed");
                    final ConnectionOptions connectionOptions = new ConnectionOptions();
                    connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.SLE);
                    connectionOptions.setTimeout(30);
                    updateConnectionOptionsInterface(connectionOptions);
                    logReceivedMessage("disconnectCardReader : ConnectionOptions : " + connectionOptions);
                    disconnectCardReader(connectionOptions);
                }
            });
        }catch (Exception e){
            logReceivedMessage(e.getMessage());
        }
    }

    // =========== TEST DCA MIFARE ==================

    @SuppressLint("CheckResult")
    private void startMifareTest() {
        logReceivedMessage("=========== TEST DCA MIFARE ==================");
        logReceivedMessage("Start Mifare test");
        logReceivedMessage("Please, tap the card");

        String key = "FFFFFFFFFFFF";

        final ConnectionResult.CardType[] cardType = {ConnectionResult.CardType.MIFARE_CLASSIC_1K};
        final String[] test3ResponseInvertedData = new String[1];
        final String[] test6ResponseInvertedData = new String[1];

        connectToCardObservable()
                .flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> {
                    cardType[0] = (connectionResult.getCardType());
                    return exchangeAPDUListObservable(generateApduList(2,
                            "Authenticate Block 6 KeyA:", ("03A0100000080601" + key)),
                            "Test ability to authenticate Sector 1 in the Mifare Classic card",
                            false);
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(3, "Read Block 6, 1 block: ",
                                "03A0110000020601"),
                        "Test the ability to read to an authenticated sector",
                        false))
                .takeWhile(list -> {
                    if (list != null && list.size() > 0 && list.get(0).length() >= 16){
                        logReceivedMessage("Device returns 16 bytes of data: Success");
                        return true;
                    } else {
                        logReceivedMessage("Device returns less than 16 bytes of data: Failed");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> {
                    byte[] responseData = ByteUtils.hexStringToByteArray(list.get(0));
                    byte[] invertedResponseData = Util.invertBytes(responseData, 0, responseData.length);
                    test3ResponseInvertedData[0] = ByteUtils.byteArrayToHexString(invertedResponseData);

                    return exchangeAPDUListObservable(
                            generateApduList(4,
                                    "Write Block 6 previous data inverted",
                                    ("03A01200001106" + test3ResponseInvertedData[0])),
                            "Test the ability to write to an authenticated sector",
                            false);
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(5,
                                "Read Block 6, 1 block: ",
                                "03A0110000020601"),
                        "Test to validate previous write was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data that matches the inverted data written in previous test
                    if (list.get(0).equals(test3ResponseInvertedData[0])) {
                        logReceivedMessage("The data matches the inverted data written in previous test, PASSED");
                        return true;
                    } else {
                        logReceivedMessage("The data don't match the inverted data written in previous test, FAILED");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(6,
                                "Read Block 14",
                                "03A0110000020E01"),
                        "Verify that automatic authentication occurs on a read to a block in a different sector",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> {
                    byte[] responseData = ByteUtils.hexStringToByteArray(list.get(0));
                    byte[] invertedResponseData = Util.invertBytes(responseData, 0, responseData.length);
                    test6ResponseInvertedData[0] = ByteUtils.byteArrayToHexString(invertedResponseData);
                    return exchangeAPDUListObservable(
                            generateApduList(7,
                                    "Write Block 14 previous data inverted",
                                    ("03A0120000110E" + test6ResponseInvertedData[0])),
                            "Verify the ability to write to an automatically authenticated block",
                            false);
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(8,
                                "Read Block 14",
                                "03A0110000020E01"),
                        "Test to validate previous write was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data that matches the inverted data written in previous test
                    if (list.get(0).equals(test6ResponseInvertedData[0])) {
                        logReceivedMessage("The data matches the inverted data written in previous test, PASSED");
                        return true;
                    } else {
                        logReceivedMessage("The data don't match the inverted data written in previous test, FAILED");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(9,
                                "Write Block 4 with 00’s",
                                "03A0120000110400000000FFFFFFFF0000000004FB04FB"),
                        "Clear the block to prepare for value tests",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(10,
                                "Read Block 4 ",
                                "03A0110000020401"),
                        "Test to validate previous write was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data all zeroes
                    if (list != null && list.size() > 0 && list.get(0).contains("00000000FFFFFFFF00000000")) {
                        logReceivedMessage("Device returns data 05000000: Passed");
                        return true;
                    } else {
                        logReceivedMessage("Device returns data 05000000: Failed");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(11,
                                "Increment Block 4 by 10 ",
                                "03A01300000604040A000000"),
                        "Verify block may be incremented",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(12,
                                "Read Block 4",
                                "03A0110000020401"),
                        "Test to validate previous increment was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data 0A000000
                    if (list != null && list.size() > 0 && list.get(0).contains("0A000000F5FFFFFF0A000000")) {
                        logReceivedMessage("Device returns data 0A000000: Passed");
                        return true;
                    } else {
                        logReceivedMessage("Device returns data 0A000000: Failed");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(13,
                                "Decrement Block 4 by 5",
                                "03A014000006040405000000"),
                        "Verify block may be decremented",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(14,
                                "Read Block 4",
                                "03A0110000020401"),
                        "Test to validate previous decrement was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data 05000000
                    if (list != null && list.size() > 0 && list.get(0).contains("05000000FAFFFFFF05000000")) {
                        logReceivedMessage("Device returns data 05000000: Passed");
                        return true;
                    } else {
                        logReceivedMessage("Device returns data 05000000: Failed");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(15,
                                "Move Block 4 to Block 5",
                                "03A0150000020405"),
                        "Verify use of the Move command",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(16, "Read Block 5",
                                "03A0110000020501"),
                        "Verify previous move was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data 05000000
                    if (list != null && list.size() > 0 && list.get(0).contains("05000000FAFFFFFF05000000")) {
                        logReceivedMessage("Device returns data 05000000: Passed");
                        return true;
                    } else {
                        logReceivedMessage("Device returns data 05000000: Failed");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> {
                    if (cardType[0] == ConnectionResult.CardType.MIFARE_CLASSIC_1K) {
                        return exchangeAPDUListObservable(
                                generateApduList(17,
                                        "1K Card - Read Block 64", "03A0110000024001"),
                                "If 1K card, test for out of range",
                                true);
                    } else {
                        return exchangeAPDUListObservable(
                                generateApduList(18,
                                        "4K Card - Read Block 64 and 255",
                                        "03A0110000024001", "03A011000002FF01"),
                                "If 4K card test range over 1K",
                                false);
                    }
                })
                .flatMap((Function<List<String>, Observable<String>>) list -> {
                    logReceivedMessage("Wait 10 sec for card disconnect");
                    return disconnectFromCardObservable();
                })
                .delay(10, TimeUnit.SECONDS)
                .flatMap((Function<String, Observable<ConnectionResult>>) disconnectResult -> connectToCardObservable())
                .takeWhile(connectionResult -> {
                    if (connectionResult.getCardType() == cardType[0]) {
                        logReceivedMessage("Device returns correct Mifare Classic card: Success");
                        return true;
                    } else {
                        logReceivedMessage("Device returns incorrect Mifare Classic card: Failed");
                        return false;
                    }
                })
                .flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> exchangeAPDUListObservable(
                        generateApduList(21,
                                "Read Block 6, 1 block: ",
                                "03A0110000020601"),
                        "Test that keys have been erased",
                        true))
                .flatMap((Function<List<String>, Observable<String>>) list -> {
                    logReceivedMessage("Wait 10 sec for card disconnect");
                    return disconnectFromCardObservable();
                })
                .delay(10, TimeUnit.SECONDS)
                .flatMap((Function<String, Observable<ConnectionResult>>) disconnectResult -> connectToCardObservable())
                .takeWhile(connectionResult -> {
                    if (connectionResult.getCardType() == cardType[0]) {
                        logReceivedMessage("Device returns correct Mifare Classic card: Success");
                        return true;
                    } else {
                        logReceivedMessage("Device returns incorrect Mifare Classic card: Failed");
                        return false;
                    }
                })
                .flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> exchangeAPDUListObservable(
                        generateApduList(24,
                                "Authenticate Block 6 KeyA:", ("03A0100000080601" + key)),
                        "Test ability to authenticate Sector 1",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(25,
                                "Read Block 6, 1 block: ",
                                "03A0110000020601"),
                        "Test the ability to read to an authenticated sector",
                        false))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<List<String>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull List<String> list) {
                        logReceivedMessage("Please, reboot the terminal. After reboot completed," +
                                " start second test button (Test mifare (after power cycle))");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                        ConnectionOptions connectionOptions = new ConnectionOptions();
                        connectionOptions.setTimeout(30);
                        updateConnectionOptionsInterface(connectionOptions);
                        disconnectCardReader(connectionOptions);
                    }

                    @Override
                    public void onComplete() {
                        ConnectionOptions connectionOptions = new ConnectionOptions();
                        connectionOptions.setTimeout(30);
                        updateConnectionOptionsInterface(connectionOptions);
                        disconnectCardReader(connectionOptions);
                    }
                });
    }

    private void startMifareTestAfterPowerCycle() {
        connectToCardObservable()
                .flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> exchangeAPDUListObservable(
                        generateApduList(27,
                        "Read Block 6, 1 block: ",
                                "03A0110000020601"),
                        "Test ability to erase keys on power cycle",
                        true))
                .flatMap((Function<List<String>, Observable<String>>) list -> {
                    logReceivedMessage("Wait 10 sec for card disconnect");
                    return disconnectFromCardObservable();
                })
                .delay(10, TimeUnit.SECONDS)
                .flatMap((Function<String, Observable<ConnectionResult>>) disconnectResult -> connectToCardObservable())
                .flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> exchangeAPDUListObservable(
                        generateApduList(31,
                                "Authenticate Block 6 KeyA:",
                                "03A0100000080601112233445566"),
                        "Test for attempt to authenticate with an invalid key ",
                        true))
                .flatMap((Function<List<String>, Observable<String>>) list -> disconnectFromCardObservable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull String s) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        ConnectionOptions connectionOptions = new ConnectionOptions();
                        connectionOptions.setTimeout(30);
                        updateConnectionOptionsInterface(connectionOptions);
                        disconnectCardReader(connectionOptions);
                    }
                });
    }

    private Observable<ConnectionResult> connectToCardObservable() {
        return Observable.create(emitter -> {
            ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setTimeout(10);
            updateConnectionOptionsInterface(connectionOptions);

            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    emitter.onNext(connectionResult);
                }

                @Override
                public void onError(PoyntError poyntError) {
                    logReceivedMessage("Connection failure: " + poyntError.toString());
                    emitter.onError(new Throwable(poyntError.toString()));
                }
            });
        });
    }

    private Observable<String> disconnectFromCardObservable() {
        return Observable.create(emitter -> {
            final ConnectionOptions connectionOptions = new ConnectionOptions();
            connectionOptions.setTimeout(10);
            updateConnectionOptionsInterface(connectionOptions);

            cardReaderService.disconnectFromCard(connectionOptions, new IPoyntDisconnectFromCardListener.Stub() {
                @Override
                public void onDisconnect() {
                    logReceivedMessage("Disconnect from card: Success");
                    emitter.onNext("Success");
                }

                @Override
                public void onError(PoyntError poyntError) {
                    logReceivedMessage("Disconnect from card Failure: " + poyntError.toString());
                    emitter.onError(new Throwable(poyntError.toString()));
                }
            });
        });
    }

    private Observable<List<String>> exchangeAPDUListObservable(final List<APDUData> apduData,
                                                                String testDescription,
                                                                boolean isCommandShouldFail) {
        return Observable.create(emitter -> cardReaderService.exchangeAPDUList(apduData, new IPoyntExchangeAPDUListListener.Stub() {
            @Override
            public void onResult(List<String> list, PoyntError poyntError) {
                if (list != null) {
                    logReceivedMessage("Exchange APDU result : " + list.toString());
                }

                if (poyntError != null) {
                    if (isCommandShouldFail) {
                        logReceivedMessage(testDescription + " Failed, Test Passed");
                        emitter.onNext(list);
                    } else {
                        logReceivedMessage(testDescription + ": Failed -> " + poyntError.toString());
                        emitter.onError(new Throwable(poyntError.toString()));
                    }
                } else {
                    String rApdu = list.size() > 0 ? list.get(0) : null;

                    if (rApdu != null) {
                        if (rApdu.endsWith("9000")) {
                            logReceivedMessage(testDescription + ": Success");
                            list.set(0, list.get(0).substring(0, list.get(0).length() - 4));
                            emitter.onNext(list);
                        } else {
                            String errorMessage = "Unknown error";
                            if (rApdu.endsWith("6300")) {
                                errorMessage = "Authentication Failed";
                            } else if (rApdu.endsWith("6501")) {
                                errorMessage = "Memory failure, unable to read/write";
                            } else if (rApdu.endsWith("6502")) {
                                errorMessage = "Not a valid value block";
                            } else if (rApdu.endsWith("6600")) {
                                errorMessage = "Incorrect address range error";
                            } else if (rApdu.endsWith("6601")) {
                                errorMessage = "Incorrect length error";
                            } else if (rApdu.endsWith("6D00")) {
                                errorMessage = "Command not allowed";
                            }
                            if (isCommandShouldFail) {
                                logReceivedMessage(testDescription + " Failed with " + errorMessage+ ", Test Passed");
                                emitter.onNext(list);
                            }else {
                                logReceivedMessage(testDescription + ": " + errorMessage);
                                emitter.onError(new Throwable(errorMessage));
                            }
                        }
                    } else {
                        logReceivedMessage(testDescription + ": Failed -> data is null");
                    }
                }
            }
        }));
    }

    private ArrayList<APDUData> generateApduList(int testNumber, String logMessage, String... apduCommands) {
        ArrayList<APDUData> apduList = new ArrayList<>();

        for (String commandApdu : apduCommands) {
            APDUData apduData = new APDUData();
            apduData.setCommandAPDU(commandApdu);
            apduData.setTimeout(30);
            updateAPDUDataInterface(apduData);

            apduList.add(apduData);
            logReceivedMessage("Process Test" + testNumber + " - APDU Exchange - " + logMessage + " " + apduData);
        }

        return apduList;
    }

    private ArrayList<APDUData> generateISOApduList(APDUData apduData, String logMessage){
        ArrayList<APDUData> apduList = new ArrayList<>();
        logReceivedMessage(logMessage +  " - APDU Exchange - " + apduData);
         apduList.add(apduData);
         return apduList;
    }

    private Observable<ConnectionResult> isoConnectToCardObservable(ConnectionOptions connectionOptions) {
        return Observable.create(emitter -> {
            cardReaderService.connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    emitter.onNext(connectionResult);
                }

                @Override
                public void onError(PoyntError poyntError) {
                    logReceivedMessage("Connection failure: " + poyntError.toString());
                    emitter.onError(new Throwable(poyntError.toString()));
                }
            });
        });
    }

    private void updateConnectionOptionsInterface(ConnectionOptions connectionOptions) {
        if (!newConnectionOptionLogic.isChecked()) {
            //nothing to do here. new logic is not enabled
            connectionOptions.setEnabledInterfaces(ConnectionOptions.INTERFACE_NONE);
            return;
        }

        Log.i(TAG, "Overriding Connection Options enabled interfaces");

        byte enabledInterfaces = ConnectionOptions.INTERFACE_NONE;
        ConnectionOptions.ContactInterfaceType contactInterfaceType = null;

        if (interfaceCL.isChecked()) {
            enabledInterfaces |= ConnectionOptions.INTERFACE_CL;
        }
        if (interfaceMSR.isChecked()) {
            enabledInterfaces |= ConnectionOptions.INTERFACE_MSR;
        }

        if (interfaceEMV.isChecked()) {
            enabledInterfaces |= ConnectionOptions.INTERFACE_CT;
            contactInterfaceType = ConnectionOptions.ContactInterfaceType.EMV;
        } else if (interfaceGSM.isChecked()) {
            enabledInterfaces |= ConnectionOptions.INTERFACE_CT;
            contactInterfaceType = ConnectionOptions.ContactInterfaceType.GSM;
        } else if (interfaceSLE.isChecked()) {
            enabledInterfaces |= ConnectionOptions.INTERFACE_CT;
            contactInterfaceType = ConnectionOptions.ContactInterfaceType.SLE;
        }

        connectionOptions.setEnabledInterfaces(enabledInterfaces);
        connectionOptions.setContactInterface(contactInterfaceType);
    }

    private void updateAPDUDataInterface(APDUData apduData) {
        if (!newConnectionOptionLogic.isChecked()) {
            //nothing to do here. new logic is not enabled
            return;
        }

        Log.i(TAG, "Overriding APDU data enabled interfaces");

        byte enabledInterfaces = APDUData.INTERFACE_NONE;
        APDUData.ContactInterfaceType contactInterfaceType = null;

        if (interfaceCL.isChecked()) {
            enabledInterfaces |= APDUData.INTERFACE_CL;
        }

        if (interfaceEMV.isChecked()) {
            enabledInterfaces |= APDUData.INTERFACE_CT;
            contactInterfaceType = APDUData.ContactInterfaceType.EMV;
        } else if (interfaceGSM.isChecked()) {
            enabledInterfaces |= APDUData.INTERFACE_CT;
            contactInterfaceType = APDUData.ContactInterfaceType.GSM;
        } else if (interfaceSLE.isChecked()) {
            enabledInterfaces |= APDUData.INTERFACE_CT;
            contactInterfaceType = APDUData.ContactInterfaceType.SLE;
        }

        apduData.setEnabledInterfaces(enabledInterfaces);
        apduData.setContactInterface(contactInterfaceType);
    }

    private void setBinRange() {
        // send setTerminalConfiguration
        byte mode = (byte) 0x00;
        // bin ranges only apply to MSR
        byte cardInterface = (byte) 0x01;
        // bin range
        String binRangeTag = "1F812F" + "0706" + etBingRange.getText().toString();
        ByteArrayOutputStream ptOs = null;
        try {
            ptOs = new ByteArrayOutputStream();
            ptOs.write(ByteUtils.hexStringToByteArray(binRangeTag));
            poyntConfigurationService.setTerminalConfiguration(mode, cardInterface,
                    ptOs.toByteArray(),
                    new IPoyntConfigurationUpdateListener.Stub() {
                        @Override
                        public void onSuccess() throws RemoteException {
                            logReceivedMessage("Bin Range Successfully updated");
                        }

                        @Override
                        public void onFailure() throws RemoteException {
                            logReceivedMessage("Bin Range updated failed!");
                        }
                    });
        } catch (Exception e) {
            logReceivedMessage("Failed to setTerminalConfiguration");
        }
    }
}
