package co.poynt.samples.codesamples.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import co.poynt.os.model.Intents;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = MyBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intents.ACTION_TRANSACTION_COMPLETED.equals(action)){
            if (intent.getExtras()!=null) {
                Log.d(TAG, "Received TRANSACTION_COMPLETED broadcast. Transaction id: " +
                        intent.getExtras().get(Intents.INTENT_EXTRAS_TRANSACTION_ID));
            }
        } else if (Intents.ACTION_PAYMENT_CANCELED.equals(action)) {
            Log.d(TAG, "Receved broadcast: PAYMENT_CANCELED");
        }
    }
}
