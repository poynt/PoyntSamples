package com.poynt.samples.paymenthooks;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import co.poynt.os.Constants;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.model.PaymentHookEvent;
import co.poynt.os.services.v1.IPoyntPaymentHooks;
import co.poynt.os.services.v1.IPoyntPaymentHooksListener;
import co.poynt.os.util.StringUtil;

public class SamplePaymentHooksService extends Service {
    private static final String TAG = SamplePaymentHooksService.class.getSimpleName();

    public SamplePaymentHooksService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IPoyntPaymentHooks.Stub mBinder = new IPoyntPaymentHooks.Stub() {
        @Override
        public void onEvent(@PaymentHookEvent.Type String s, Payment payment, Bundle bundle, IPoyntPaymentHooksListener listener) throws RemoteException {
            Log.d(TAG, "event: " + s + " received");

            if(bundle != null) {
                for (String key : bundle.keySet()) {
                    Log.d(TAG, key + " " + bundle.get(key));
                }
            }

            if (PaymentHookEvent.PAYMENT_METHOD_SELECTED.equals(s)) {
                if (bundle == null) {
                    listener.onContinue();
                    return;
                }

                String paymentType = bundle.getString(Constants.CardExtras.PAYMENT_TYPE);

                if (StringUtil.isEmpty(paymentType)) {
                    listener.onContinue();
                    return;
                }

                if (payment.getAmount() == 500) {
                    payment.setAmount(100);
                    listener.updatePayment(payment);
                } else if (payment.getAmount() == 1000) {
                    listener.onContinue();
                } else {
                    Intent intent = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK);
                    intent.setComponent(new ComponentName(getPackageName(), MainActivity.class.getName()));
                    intent.putExtra("payment", payment);

                    listener.onLaunchActivity(intent);
                }
            } else if (PaymentHookEvent.PAYMENT_AUTHORIZED.equals(s)) {
                listener.onContinue();
            } else {
                listener.onContinue();
            }
        }
    };
}
