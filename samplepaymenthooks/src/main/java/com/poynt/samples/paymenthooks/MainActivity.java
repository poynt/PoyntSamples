package com.poynt.samples.paymenthooks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    Payment payment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button setNewAmount = (Button) findViewById(R.id.set1usd);
        setNewAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payment.setAmount(100);

                Intent result = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK_RESULT);
                result.putExtra("payment", payment);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

        Button noChanges = (Button) findViewById(R.id.noChanges);
        noChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent result = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK_RESULT);
                result.putExtra("payment", payment);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

        Button closeNoPayment = (Button) findViewById(R.id.closeNoPayment);
        closeNoPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent result = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK_RESULT);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

        // Get the intent that started this activity
        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
        } else {
            Log.e(TAG, "Activity launched with no intent!");
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        if (Intents.ACTION_PROCESS_PAYMENT_HOOK.equals(action)) {
            payment = intent.getParcelableExtra("payment");
            if (payment == null) {
                Log.e(TAG, "launched with no payment object");
                Intent result = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK_RESULT);
                setResult(Activity.RESULT_CANCELED, result);
                finish();
            }
        } else {
            Log.e(TAG, "launched with unknown intent action");
            Intent result = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK_RESULT);
            setResult(Activity.RESULT_CANCELED, result);
            finish();
        }
    }
}
