package com.poynt.samples.paymenthooks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionReference;
import co.poynt.api.model.TransactionReferenceType;
import co.poynt.os.Constants;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;

public class AfterAuthActivity extends Activity {

    private static final String TAG = AfterAuthActivity.class.getSimpleName();

    Payment payment;
    Transaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.after_auth_activity);

        Button noChanges = (Button) findViewById(R.id.noChanges);
        noChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent result = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK_RESULT);
                result.putExtra(Constants.HooksExtras.PAYMENT, payment);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

        Button closeAddData = (Button) findViewById(R.id.closeAddData);
        closeAddData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransactionReference reference = new TransactionReference();
                reference.setId(UUID.randomUUID().toString());
                reference.setType(TransactionReferenceType.CUSTOM);

                if(payment.getTransactions() == null || payment.getTransactions().size() == 0){
                    transaction.setReferences(Arrays.asList(reference));
                    payment.setTransactions(Arrays.asList(transaction));
                } else {
                    for(Transaction t : payment.getTransactions()){
                        if(transaction.getId() == t.getId()){
                            t.setReferences(Arrays.asList(reference));
                            break;
                        }
                    }
                }

                Map<String, String> data = payment.getProcessorOptions() == null
                        ? new HashMap<String, String>() : payment.getProcessorOptions();
                data.put(transaction.getId().toString(), "processed");
                payment.setProcessorOptions(data);

                Intent result = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK_RESULT);
                result.putExtra(Constants.HooksExtras.PAYMENT, payment);
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
            payment = intent.getParcelableExtra(Constants.HooksExtras.PAYMENT);
            transaction = intent.getParcelableExtra(Constants.HooksExtras.TRANSACTION);

            boolean noData = false;

            if (payment == null) {
                Log.e(TAG, "launched with no payment object");
                noData = true;
            }

            if (transaction == null) {
                Log.e(TAG, "launched with no transaction object");
                noData = true;
            }

            if (noData) {
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
