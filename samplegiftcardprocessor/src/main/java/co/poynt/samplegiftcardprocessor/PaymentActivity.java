package co.poynt.samplegiftcardprocessor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import co.poynt.api.model.Transaction;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;

public class PaymentActivity extends Activity implements
        CustomPaymentFragment.OnFragmentInteractionListener {

    private static final String TAG = "PaymentActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Get the intent that started this activity
        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
        } else {
            Log.e(TAG, "PaymentActivity launched with no intent!");
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

    }


    @Override
    public void onFragmentInteraction(Transaction transaction, PoyntError error) {
        // Create intent to deliver some kind of result data
        Intent result = new Intent(Intents.ACTION_COLLECT_PAYMENT_RESULT);
        result.putExtra("transaction", transaction);
        result.putExtra("error", error);
        if (error == null) {
            setResult(Activity.RESULT_OK, result);
        }else{
            setResult(Activity.RESULT_CANCELED, result);
        }
        finish();
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        if ("COLLECT_CUSTOM_PAYMENT".equals(action)) {
            final Transaction transaction = intent.getParcelableExtra("transaction");
            if (transaction == null) {
                Log.e(TAG, "PaymentActivity launched with no payment object");
                Intent result = new Intent(Intents.ACTION_COLLECT_PAYMENT_RESULT);
                setResult(Activity.RESULT_CANCELED, result);
                finish();
            } else {
                CustomPaymentFragment customPaymentFragment =
                        CustomPaymentFragment.newInstance(transaction);
                // prevent the merchant from dismissing the payment fragment by taping
                // anywhere on the screen
                customPaymentFragment.setCancelable(false);
                Log.d(TAG, "loading custom payment fragment");
                getFragmentManager().beginTransaction()
                        .add(R.id.container, customPaymentFragment)
                        .commit();
            }
        }
    }

}
