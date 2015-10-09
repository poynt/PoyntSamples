package co.poynt.samples.custompayment;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

import co.poynt.api.model.Card;
import co.poynt.api.model.CardType;
import co.poynt.api.model.FundingSource;
import co.poynt.api.model.Transaction;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntCustomTransactionService;
import co.poynt.os.services.v1.IPoyntCustomTransactionServiceListener;
import timber.log.Timber;

/**
 * Created by sathyaiyer on 8/6/15.
 */
public class CardEntryActivity extends Activity {

    private TextView amountView;
    private Transaction transaction = null;
    Button cancelButton;
    Button doneButton;
    EditText cardInfo;
    Intent originalIntent = null;
    Intent resultIntent = null;

    private IPoyntCustomTransactionService paymentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        originalIntent = getIntent();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.poynt_gift_card);

        if (originalIntent != null) {
            transaction = originalIntent.getParcelableExtra("transaction");
            if (transaction == null) {
                Timber.e(" Null transaction passed");
            }
        }
        amountView = (TextView) findViewById(R.id.customPaymentAmount);
        doneButton = (Button) findViewById(R.id.yes);
        cardInfo = (EditText) findViewById(R.id.cardnumber);

        if (transaction != null) {
            setTotal(transaction.getAmounts().getTransactionAmount());
            doneButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            validateCardInfo();
                            sendResult();
                        }
                    }
            );
            Intent serviceIntent = new Intent();
            serviceIntent.setClass(this, CustomPaymentService.class);
            bindService(serviceIntent,
                    serviceConnection,
                    Context.BIND_AUTO_CREATE);
        } else {
            amountView.setText(" Invalid transaction passed");
            amountView.setTextColor(Color.RED);
            doneButton.setVisibility(View.GONE);

        }

        cancelButton = (Button) findViewById(R.id.no);
        cancelButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED, originalIntent);
                        finish();
                    }
                }
        );
    }


    private boolean validateCardInfo() {
        String cardEntered = cardInfo.getText().toString();
        if (cardEntered.length() < 4) {
            return false;
        }
        return true;
    }

    private void sendResult() {
        String cardEntered = cardInfo.getText().toString();
        if (cardEntered != null && cardEntered.length() > 4) {
            Card card = new Card();
            card.setType(CardType.OTHER);
            card.setNumber(cardEntered);
            card.setCardHolderFullName("Poynt User");
            FundingSource fundingSource = transaction.getFundingSource();
            fundingSource.setCard(card);
            transaction.setFundingSource(fundingSource);
            resultIntent = new Intent();
            resultIntent.putExtra("transaction", transaction);

            // get the transaction authorized.
            String requestId = UUID.randomUUID().toString();
            try {
                paymentService.authorizeTransaction(requestId,
                        transaction,
                        paymentServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
                finishWithError();
            }

        } else {
            finishWithError();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (paymentService != null) {
            unbindService(serviceConnection);
        }
    }

    private void finishWithError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultIntent = new Intent();
                resultIntent.putExtra("transaction", transaction);
                setResult(RESULT_CANCELED, resultIntent);
                finish();
            }
        });
    }

    private void finishWithSuccess(final Transaction transaction) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultIntent = new Intent();
                resultIntent.putExtra("transaction", transaction);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }


    public void setTotal(long total) {
        StringBuilder strTotal = new StringBuilder();
        strTotal.append(total);
        BigDecimal modelVal = new BigDecimal(strTotal.toString());
        modelVal = modelVal.divide(BigDecimal.valueOf(100));
        BigDecimal displayVal = modelVal.setScale(2, RoundingMode.HALF_UP);
        String amountStr = NumberFormat.getCurrencyInstance(Locale.US).format(displayVal.doubleValue());
        amountView.setText(amountStr);
    }

    private IPoyntCustomTransactionServiceListener paymentServiceListener = new IPoyntCustomTransactionServiceListener.Stub() {
        @Override
        public void onSuccess(String requestId, String message, Transaction transaction) throws RemoteException {
            finishWithSuccess(transaction);
        }

        @Override
        public void onError(String s, PoyntError poyntError) throws RemoteException {
            finishWithError();
        }

        @Override
        public void onLaunchActivity(Intent intent, String requestId) throws RemoteException {

        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            paymentService = IPoyntCustomTransactionService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            paymentService = null;
        }
    };
}
