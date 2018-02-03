package co.poynt.samplegiftcardprocessor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

import co.poynt.api.model.Transaction;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.services.v1.IPoyntConfigurationService;
import co.poynt.os.services.v1.IPoyntConfigurationUpdateListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();


    private static final int COLLECT_PAYMENT_REQUEST = 13132;
    private TextView mDumpTextView;
    private ScrollView mScrollView;
    private IPoyntConfigurationService poyntConfigurationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        Button readCardBtn = (Button) findViewById(R.id.readCardBtn);
        readCardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPoyntPayment();
            }
        });
        Button setCustomBinRange = (Button) findViewById(R.id.set_bin_range);
        setCustomBinRange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send setTerminalConfiguration
                byte mode = (byte) 0x00;
                // bin ranges only apply to MSR
                byte cardInterface = (byte) 0x01;
                // bin range
                String binRange = "0706000000111111";
                ByteArrayOutputStream ptOs = null;
                try {
                    ptOs = new ByteArrayOutputStream();
                    ptOs.write(fromString("1F812F"));
                    ptOs.write(fromString(binRange));
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
                } catch (IOException e) {
                    logReceivedMessage("Received IOException - please check your data");
                } catch (RemoteException e) {
                    logReceivedMessage("Failed to communicate with PoyntConfigurationService");
                } finally {
                    try {
                        if (ptOs != null) {
                            ptOs.close();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(poyntConfigurationServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_CONFIGURATION_SERVICE),
                poyntConfigurationServiceConnection, Context.BIND_AUTO_CREATE);
//        bindService(new Intent(IPoyntConfigurationService.class.getName()),
//                poyntConfigurationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void launchPoyntPayment() {
        Locale locale = new Locale("en", "US");
        String currencyCode = NumberFormat.getCurrencyInstance(locale).getCurrency().getCurrencyCode();

        Payment payment = new Payment();
        String referenceId = UUID.randomUUID().toString();
        payment.setReferenceId(referenceId);
        payment.setCurrency(currencyCode);
        payment.setReadCardDataOnly(true);

        // start Payment activity for result
        try {
            Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
            collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, payment);
            startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_REQUEST);
        } catch (ActivityNotFoundException ex) {
            Log.e("ConfigurationTest", "Poynt Payment Activity not found - did you install PoyntServices?", ex);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);
                Log.d("ConfigurationTest", "Received onPaymentAction from PaymentFragment w/ Status:" + payment.getStatus());

                if (payment.getTransactions() != null && payment.getTransactions().size() > 0) {
                    Transaction transaction = payment.getTransactions().get(0);
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    Type transactionType = new TypeToken<Transaction>() {
                    }.getType();
                    logReceivedMessage(gson.toJson(transaction, transactionType));
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            logReceivedMessage("Payment Canceled");
        }
    }

    public void logReceivedMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDumpTextView.append("<< " + message + "\n\n");
                mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
                Log.d(TAG, "run: " + message);
            }
        });
    }

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

    public byte[] fromString(String pData) {
        if (pData == null) {
            throw new IllegalArgumentException("Argument can\'t be null");
        } else {
            String text = pData.replace(" ", "");
            if (text.length() % 2 != 0) {
                throw new IllegalArgumentException("Hex binary needs to be even-length :" + pData);
            } else {
                byte[] commandByte = new byte[Math.round((float) text.length() / 2.0F)];
                int j = 0;

                for (int i = 0; i < text.length(); i += 2) {
                    Integer val = Integer.valueOf(Integer.parseInt(text.substring(i, i + 2), 16));
                    commandByte[j++] = val.byteValue();
                }

                return commandByte;
            }
        }
    }
}
