package co.poynt.samplegiftcardprocessor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;
import java.lang.reflect.Type;

import co.poynt.api.model.Transaction;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();


    private static final int COLLECT_PAYMENT_REQUEST = 13132;
    private TextView mDumpTextView;
    private ScrollView mScrollView;

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
                    Type transactionType = new TypeToken<Transaction>(){}.getType();
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
            }
        });
    }
}
