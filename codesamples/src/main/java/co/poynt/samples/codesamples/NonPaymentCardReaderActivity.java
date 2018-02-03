package co.poynt.samples.codesamples;

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
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import co.poynt.os.model.APDUData;
import co.poynt.os.model.ConnectionOptions;
import co.poynt.os.model.ConnectionResult;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntCardInsertListener;
import co.poynt.os.services.v1.IPoyntCardReaderService;
import co.poynt.os.services.v1.IPoyntConnectToCardListener;
import co.poynt.os.services.v1.IPoyntDisconnectFromCardListener;
import co.poynt.os.services.v1.IPoyntExchangeAPDUListener;

public class NonPaymentCardReaderActivity extends Activity {
    private IPoyntCardReaderService cardReaderService;
    private static final String TAG = NonPaymentCardReaderActivity.class.getName();
    private ProgressDialog cardRemovalProgress;

    private UIEventReceiver uiEventReceiver;

    private TextView mDumpTextView;
    private ScrollView mScrollView;
    private EditText apduDataInput;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_read);
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Button clearLogBtn = (Button) findViewById(R.id.clearLog);
        clearLogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLog();
            }
        });

        final Button connectToCardBtn = (Button) findViewById(R.id.connectToCard);
        connectToCardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        ConnectionOptions connectionOptions = new ConnectionOptions();
                        connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.GSM);
                        connectionOptions.setTimeout(60);
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
                        connectionOptions.setContactInterface(ConnectionOptions.ContactInterfaceType.GSM);
                        connectionOptions.setTimeout(60);
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
    }

    protected void onPause() {
        super.onPause();
        // always change the mode

        unregisterReceiver(uiEventReceiver);
        unbindService(serviceConnection);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDumpTextView.append(message + "\n\n");
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

    private void selectMasterfile(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = new APDUData();
        apduData.setContactInterface(APDUData.ContactInterfaceType.GSM);
        apduData.setTimeout(60);
        apduData.setCommandAPDU("04A0A40000023F0000");
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
        try {
            cardReaderService.exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }
}