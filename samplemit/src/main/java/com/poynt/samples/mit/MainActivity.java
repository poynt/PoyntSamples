package com.poynt.samples.mit;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import co.poynt.api.model.Business;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.model.PaymentSettings;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntBusinessReadListener;

import static co.poynt.os.model.Intents.ACTION_PROCESS_VOID;
import static co.poynt.os.model.Intents.INTENT_EXTRAS_TRANSACTION_AMOUNT;
import static co.poynt.os.model.Intents.INTENT_EXTRAS_TRANSACTION_AUTH_CODE;
import static co.poynt.os.model.Intents.INTENT_EXTRAS_TRANSACTION_ID;
import static co.poynt.os.model.Intents.INTENT_EXTRAS_TRANSACTION_PAN_LAST4;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PRE_AUTH_ACTION = 112200;
    private static final int INCREMENT_ACTION = 112201;
    private static final int COMPLETE_ACTION = 112202;
    private static final int CANCEL_ACTION = 112203;
    private static final int VERIFY_ACTION = 112204;
    private static final int NO_SHOW_ACTION = 112205;
    private static final int DELAYED_CHARGE_ACTION = 112206;
    private static final int CANCEL_AUTH_ACTION = 112207;

    private Payment payment;

    private PaymentSettings paymentSettings;
    private BusinessProvider businessProvider;
    private boolean connectedToPoyntServices;

    private Button preAuth;
    private Button verify;
    private Button increment;
    private Button complete;
    private Button cancel;
    private Button noShow;
    private Button noShowTrx;
    private Button delayedCharge;
    private Button delayedChargeTrx;
    private Button cancelAuth;

    private EditText preAuthAmount;
    private EditText authCode;
    private EditText lastFour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preAuth = findViewById(R.id.preAuth);
        verify = findViewById(R.id.verify);
        increment = findViewById(R.id.increment);
        complete = findViewById(R.id.complete);
        cancel = findViewById(R.id.cancel);
        noShow = findViewById(R.id.noShow);
        noShowTrx = findViewById(R.id.noShowTrx);
        delayedCharge = findViewById(R.id.delayedCharge);
        delayedChargeTrx = findViewById(R.id.delayedChargeTrx);
        cancelAuth = findViewById(R.id.cancelAuth);

        preAuthAmount = findViewById(R.id.preAuthAmount);
        authCode = findViewById(R.id.authCode);
        lastFour = findViewById(R.id.lastFour);
        preAuth.setOnClickListener(view -> {
            String cleanString = preAuthAmount.getText().toString()
                    .replaceAll("[$,.]", "");

            Long result = 123L;

            if (!TextUtils.isEmpty(cleanString)) {
                result = Long.parseLong(cleanString);
            }

            Bundle extras = new Bundle();
            extras.putLong(INTENT_EXTRAS_TRANSACTION_AMOUNT, result);
            preparePaymentObject(PRE_AUTH_ACTION, Intents.ACTION_COLLECT_PRE_AUTH, extras);
        });

        verify.setOnClickListener(view -> {
            preparePaymentObject(VERIFY_ACTION, Intents.ACTION_VERIFY_CARD, null);
        });

        increment.setOnClickListener(view -> {
            preparePaymentObject(INCREMENT_ACTION, Intents.ACTION_PROCESS_INCREMENT, getExtras());
        });

        complete.setOnClickListener(view -> {
            preparePaymentObject(COMPLETE_ACTION, Intents.ACTION_PROCESS_CAPTURE, getExtras());
        });

        cancel.setOnClickListener(view -> {
            preparePaymentObject(CANCEL_ACTION, Intents.ACTION_PROCESS_VOID, getExtras());
        });

        noShow.setOnClickListener(view -> {
            preparePaymentObject(NO_SHOW_ACTION, Intents.ACTION_PROCESS_NO_SHOW, getAuthCodeExtras());
        });

        delayedCharge.setOnClickListener(view -> {
            preparePaymentObject(DELAYED_CHARGE_ACTION, Intents.ACTION_PROCESS_DELAYED_CHARGE, getAuthCodeExtras());
        });

        cancelAuth.setOnClickListener(view -> {
            preparePaymentObject(CANCEL_ACTION, ACTION_PROCESS_VOID, getAuthCodeExtras());
        });

        noShowTrx.setOnClickListener(view -> {
            preparePaymentObject(NO_SHOW_ACTION, Intents.ACTION_PROCESS_NO_SHOW, getExtras());
        });

        delayedChargeTrx.setOnClickListener(view -> {
            preparePaymentObject(DELAYED_CHARGE_ACTION, Intents.ACTION_PROCESS_DELAYED_CHARGE, getExtras());
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        bindToServices();
        businessProvider.getBusiness(new IPoyntBusinessReadListener.Stub() {
            @Override
            public void onResponse(Business business, PoyntError poyntError) throws RemoteException {
                if (business != null) {
                    paymentSettings = PaymentSettings.create(business);

                    runOnUiThread(() -> updateViews());
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unBindFromServices();
    }

    protected synchronized void bindToServices() {
        if (!connectedToPoyntServices) {
            businessProvider = new BusinessProvider(this);
            connectedToPoyntServices = true;
        }
    }

    protected synchronized void unBindFromServices() {
        if (connectedToPoyntServices) {
            businessProvider.finish();
            connectedToPoyntServices = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (resultCode == RESULT_OK) {
            if (requestCode != VERIFY_ACTION && requestCode != NO_SHOW_ACTION && requestCode != DELAYED_CHARGE_ACTION) {
                payment = data.getParcelableExtra("PAYMENT");

                updateViews();
            }
        } else {
            Log.d(TAG, "Result canceled");
        }
    }

    private void updateViews() {
        verify.setEnabled(paymentSettings.isEnableVerifyCard());

        noShow.setEnabled(paymentSettings.isEnableMIT());
        delayedCharge.setEnabled(paymentSettings.isEnableMIT());
        cancelAuth.setEnabled(paymentSettings.isEnableMIT());

        if (payment != null) {
            TransactionStatus status = null;
            if (payment.getTransactions() != null && payment.getTransactions().get(0) != null) {
                status = payment.getTransactions().get(0).getStatus();
            }

            increment.setEnabled(TransactionStatus.AUTHORIZED == status);
            complete.setEnabled(TransactionStatus.AUTHORIZED == status);
            cancel.setEnabled(TransactionStatus.AUTHORIZED == status || TransactionStatus.CAPTURED == status);

            noShowTrx.setEnabled(TransactionStatus.AUTHORIZED == status && paymentSettings.isEnableMIT());
            delayedChargeTrx.setEnabled(TransactionStatus.CAPTURED == status && paymentSettings.isEnableMIT());
        }
    }

    private Bundle getExtras() {
        if (payment == null) {
            return null;
        }

        String tid = this.payment.getTransactions().get(0).getId().toString();

        Bundle extras = new Bundle();
        extras.putString(INTENT_EXTRAS_TRANSACTION_ID, tid);

        return extras;
    }

    private Bundle getAuthCodeExtras() {
        Bundle extras = new Bundle();

        String authCodeStr = authCode.getText().toString();
        String lastFourStr = lastFour.getText().toString();

        extras.putString(INTENT_EXTRAS_TRANSACTION_AUTH_CODE, authCodeStr);
        extras.putString(INTENT_EXTRAS_TRANSACTION_PAN_LAST4, lastFourStr);

        return extras;
    }

    private void preparePaymentObject(int requestCode, String action, Bundle extras) {
        try {
            Intent intent = new Intent(action);
            if (extras != null) {
                intent.putExtras(extras);
            }
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException ex) {
            Log.d(TAG, "There is no activity to handle the action");
        }
    }

}
