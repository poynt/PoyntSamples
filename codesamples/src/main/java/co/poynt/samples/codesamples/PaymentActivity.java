package co.poynt.samples.codesamples;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.poynt.api.model.Card;
import co.poynt.api.model.CardType;
import co.poynt.api.model.FundingSourceAccountType;
import co.poynt.api.model.Order;
import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionAction;
import co.poynt.api.model.TransactionReference;
import co.poynt.api.model.TransactionReferenceType;
import co.poynt.os.contentproviders.orders.transactionreferences.TransactionreferencesColumns;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.model.PaymentStatus;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntOrderService;
import co.poynt.os.services.v1.IPoyntOrderServiceListener;
import co.poynt.os.services.v1.IPoyntTransactionService;
import co.poynt.os.services.v1.IPoyntTransactionServiceListener;
import co.poynt.samples.codesamples.utils.Util;

public class PaymentActivity extends Activity {

    // request code for payment service activity
    private static final int COLLECT_PAYMENT_WITH_TIP_REQUEST = 13131;
    private static final int COLLECT_PAYMENT_REQUEST = 13132;
    private static final int ZERO_DOLLAR_AUTH_REQUEST = 13133;
    private static final int COLLECT_PAYMENT_REFS_REQUEST = 13134;
    private static final String TAG = PaymentActivity.class.getSimpleName();

    private IPoyntTransactionService mTransactionService;
    private IPoyntOrderService mOrderService;

    Button chargeBtn;
    Button payOrderBtn;
    Button payWithRefsBtn;
    Button zeroDollarAuthBtn;
    Button launchAndCancelBtn;
    Button nonRefCredit;
    Button payOrderWithTipBtn;
    Button payCashWithTip;
    TextView orderSavedStatus;
    TextView payWithRefsResult;
    TextView tipStatus;

    String lastReferenceId;


    /*
     * Class for interacting with the OrderService
     */
    private ServiceConnection mOrderServiceConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "PoyntOrderService is now connected");
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mOrderService = IPoyntOrderService.Stub.asInterface(service);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "PoyntOrderService has unexpectedly disconnected");
            mOrderService = null;
        }
    };
    private IPoyntOrderServiceListener saveOrderCallback = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(Order order, String s, PoyntError poyntError) throws RemoteException {
            if (order == null) {
                Log.d("orderListener", "poyntError: " + (poyntError == null ? "" : poyntError.toString()));
            }else{
                Log.d(TAG, "orderResponse: " + order);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        orderSavedStatus.setText("ORDER SAVED");
                    }
                });
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        android.app.ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        chargeBtn = (Button) findViewById(R.id.chargeBtn);
        chargeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPoyntPayment(100l,false, null);
            }
        });

        chargeBtn.setEnabled(true);

        orderSavedStatus = (TextView) findViewById(R.id.orderSavedStatus);


        payOrderBtn = (Button) findViewById(R.id.payOrderBtn);
        payOrderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Order order = Util.generateOrder();
                launchPoyntPayment(order.getAmounts().getNetTotal(), false, order);
            }
        });

        Button launchTxnList = (Button) findViewById(R.id.launchTxnList);
        launchTxnList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("poynt.intent.action.VIEW_TRANSACTIONS");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        zeroDollarAuthBtn = (Button) findViewById(R.id.zeroDollarAuthBtn);
        zeroDollarAuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doZeroDollarAuth();
            }
        });

        payWithRefsBtn = (Button) findViewById(R.id.payWithRefs);
        payWithRefsResult = (TextView) findViewById(R.id.payWithRefsResult);
        payWithRefsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                paymentWithCustomRefs();
            }
        });

        launchAndCancelBtn = (Button) findViewById(R.id.launchAndCancelBtn);
        launchAndCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Payment p = new Payment();
                // disable EMV contact
                p.setDisableEMVCT(true);
                p.setAmount(1000L);
                p.setCurrency("USD");

                Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
                collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, p);

                Handler h = new Handler(getMainLooper());
                // Cancel payment fragment after 2 seconds
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Intents.ACTION_CANCEL_PAYMENT_FRAGMENT);
                        // Only the app that started Payment fragment can cancel it.
                        intent.putExtra(Intents.INTENT_EXTRAS_CALLER_PACKAGE_NAME, getPackageName());
                        sendBroadcast(intent);
                    }
                }, 2000L);


                startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_REQUEST);
            }
        });

        nonRefCredit = (Button) findViewById(R.id.nonRefCredit);
        nonRefCredit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Payment p = new Payment();
                p.setCurrency("USD");
                p.setNonReferencedCredit(true);
                p.setAmount(1000L);
                Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
                collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, p);
                startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_REQUEST);

            }
        });
        // Pay for order with Tip and check for tip amounts in transaction, tip map
        payOrderWithTipBtn = (Button) findViewById(R.id.payOrderWithTipBtn);
        tipStatus = (TextView) findViewById(R.id.tipStatus);
        payOrderWithTipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Order order = Util.generateOrder();
                launchPoyntPayment(0l, true, order);
            }
        });

        // Pay with cash for a transaction with tip
        payCashWithTip = (Button) findViewById(R.id.cashTxnWithTip);
        payCashWithTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Payment cashPayment = new Payment();
                cashPayment.setAmount(1000l);
                cashPayment.setTipAmount(200l);
                cashPayment.setCashOnly(true);
                Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
                collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, cashPayment);
                startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_REQUEST);
            }
        });

/*

        launchRegisterBtn = (Button) findViewById(R.id.launchRegisterBtn);
        // Only works if Poynt Register does not have an active order in progress
        launchRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Product product = Util.createProduct();
                Intent intent = new Intent();
                intent.setAction(Intents.ACTION_ADD_PRODUCT_TO_CART);
                intent.putExtra(Intents.INTENT_EXTRA_PRODUCT, product);
                intent.putExtra(Intents.INTENT_EXTRA_QUANTITY, 2.0f);
                startActivity(intent);
            }
        });
*/
    }
    private void paymentWithCustomRefs(){
        Payment payment = new Payment();
        payment.setCurrency("USD");
        payment.setAmount(200l);
        payment.setReferences(generateReferences());
        payment.setSkipSignatureScreen(true);
        payment.setSkipReceiptScreen(true);
        payment.setSkipPaymentConfirmationScreen(true);
        Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
        collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, payment);
        startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_REFS_REQUEST);
    }

    private List<TransactionReference> generateReferences(){
        List<TransactionReference> transactionReferences = new ArrayList<>();
        TransactionReference transactionReference1 = new TransactionReference();
        transactionReference1.setCustomType("posReferenceId");
        transactionReference1.setType(TransactionReferenceType.CUSTOM);
        transactionReference1.setId("12345");

        transactionReferences.add(transactionReference1);
        return transactionReferences;
    }
    private void doZeroDollarAuth() {
        Payment p = new Payment();
        p.setAction(TransactionAction.VERIFY);
        p.setCurrency("USD");
        p.setAuthzOnly(true);

        Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
        collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, p);
        startActivityForResult(collectPaymentIntent, ZERO_DOLLAR_AUTH_REQUEST);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindServices();
    }

    private void bindServices() {
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_ORDER_SERVICE),
                mOrderServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_TRANSACTION_SERVICE),
                mTransactionServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindServices();
    }

    private void unbindServices() {
        unbindService(mOrderServiceConnection);
        unbindService(mTransactionServiceConnection);
    }

    private class SaveOrderTask extends AsyncTask<Order, Void, Void> {
        protected Void doInBackground(Order... params) {
            Order order = params[0];
            String requestId = UUID.randomUUID().toString();
            if (mOrderService != null) {
                try {
                    mOrderService.createOrder(order, requestId, saveOrderCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_payment, menu);
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

    private IPoyntTransactionServiceListener mTransactionServiceListener = new IPoyntTransactionServiceListener.Stub() {
        public void onResponse(Transaction _transaction, String s, PoyntError poyntError) throws RemoteException {
            Gson gson = new Gson();
            Type transactionType = new TypeToken<Transaction>() {
            }.getType();
            String transactionJson = gson.toJson(_transaction, transactionType);
            Log.d(TAG, "onResponse: " + transactionJson);
            Log.d(TAG, "onResponse: " + _transaction);

        }

        //@Override
        public void onLaunchActivity(Intent intent, String s) throws RemoteException {
            //do nothing
        }

        public void onLoginRequired() throws RemoteException {
            Log.d(TAG, "onLoginRequired called");
        }

    };

    /**
     * Get a transaction using Id
     * This is an async call, the response is returned in a callback
     * @param txnId transaction Id
     */
    public void getTransaction(String txnId) {
        if(mTransactionService!=null) {
            try {
                mTransactionService.getTransaction(txnId, UUID.randomUUID().toString(),
                        mTransactionServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private ServiceConnection mTransactionServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mTransactionService = IPoyntTransactionService.Stub.asInterface(iBinder);
            Log.d(TAG, "Transaction service connected");
        }

        public void onServiceDisconnected(ComponentName componentName) {
            mTransactionService = null;
            Log.d(TAG, "Transaction service disconnected");
        }
    };

    private void launchPoyntPayment(long amount, boolean collectTip, Order order) {
        String currencyCode = NumberFormat.getCurrencyInstance().getCurrency().getCurrencyCode();

        Payment payment = new Payment();
        lastReferenceId = UUID.randomUUID().toString();
        TransactionReference lastReference = new TransactionReference();
        lastReference.setType(TransactionReferenceType.CUSTOM);
        lastReference.setId(lastReferenceId);
        lastReference.setCustomType("transactionReference");
        payment.setReferences(Collections.singletonList(lastReference));

        payment.setCurrency(currencyCode);
        // enable multi-tender in payment options
        payment.setMultiTender(true);

        if (order != null) {
            payment.setOrder(order);
            payment.setOrderId(order.getId().toString());

            // tip can be preset
            //payment.setTipAmount(500l);
            payment.setAmount(order.getAmounts().getNetTotal());
        } else {
            // some random amount
            payment.setAmount(1200l);

            // here's how tip can be disabled for tip enabled merchants
            // payment.setDisableTip(true);
        }

        payment.setSkipSignatureScreen(true);
        payment.setSkipReceiptScreen(true);
        payment.setSkipPaymentConfirmationScreen(true);

        payment.setCallerPackageName("co.poynt.sample");
        Map<String, String> processorOptions = new HashMap<>();
        processorOptions.put("installments", "2");
        processorOptions.put("type", "emi");
        processorOptions.put("originalAmount", "2400");
        payment.setProcessorOptions(processorOptions);

        // start Payment activity for result
        try {
            Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
            collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, payment);
            if(collectTip){
                startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_WITH_TIP_REQUEST);
            }else {
                startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_REQUEST);
            }
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Poynt Payment Activity not found - did you install PoyntServices?", ex);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "Received onActivityResult (" + requestCode + ")");
        // Check which request we're responding to
        if (requestCode == COLLECT_PAYMENT_REQUEST) {
            logData("Received onActivityResult from Payment Action");
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);

                    if (payment != null) {
                        //save order
                        if (payment.getOrder() != null) {
                            new SaveOrderTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, payment.getOrder());
                        }

//                      Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        Gson gson = new Gson();
//                        Type paymentType = new TypeToken<Payment>() {
//                        }.getType();
//                        Log.d(TAG, gson.toJson(payment, paymentType));
                        Log.d(TAG, "onActivityResult: " + payment.getTransactions().get(0));
                        for (Transaction t : payment.getTransactions()) {
                            Type txnType = new TypeToken<Transaction>() {
                            }.getType();
                            Log.d(TAG, "onActivityResult: transaction: " + gson.toJson(t, txnType));
                            FundingSourceAccountType fsAccountType = t.getFundingSource().getAccountType();
                            if (t.getFundingSource().getCard() != null) {
                                Card c = t.getFundingSource().getCard();
                                String numberMasked = c.getNumberMasked();
                                String approvalCode = t.getApprovalCode();
                                CardType cardType = c.getType();
                                switch (cardType) {
                                    case AMERICAN_EXPRESS:
                                        // amex
                                        break;
                                    case VISA:
                                        // visa
                                        break;
                                    case MASTERCARD:
                                        // MC
                                        break;
                                    case DISCOVER:
                                        // discover
                                        break;
                                }
                            }

                        }

                        Log.d(TAG, "Received onPaymentAction from PaymentFragment w/ Status("
                                + payment.getStatus() + ")");
                        if (payment.getStatus().equals(PaymentStatus.COMPLETED)) {
                            logData("Payment Completed");
                        } else if (payment.getStatus().equals(PaymentStatus.AUTHORIZED)) {
                            logData("Payment Authorized");
                        } else if (payment.getStatus().equals(PaymentStatus.CANCELED)) {
                            logData("Payment Canceled");
                        } else if (payment.getStatus().equals(PaymentStatus.FAILED)) {
                            logData("Payment Failed");
                        } else if (payment.getStatus().equals(PaymentStatus.REFUNDED)) {
                            logData("Payment Refunded");
                        } else if (payment.getStatus().equals(PaymentStatus.VOIDED)) {
                            logData("Payment Voided");
                        } else {
                            logData("Payment Completed");
                        }
                    } else {
                        // This should not happen, but in case it does, handle it using Content Provider
                        getTransactionFromContentProvider();
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                logData("Payment Canceled");
            }
        } else if (requestCode == ZERO_DOLLAR_AUTH_REQUEST) {
            Log.d(TAG, "onActivityResult: $0 auth request");
            if (resultCode == Activity.RESULT_OK) {
                Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);
                Gson gson = new Gson();
                Type paymentType = new TypeToken<Payment>() {
                }.getType();
                Log.d(TAG, gson.toJson(payment, paymentType));
            }
        } else if (requestCode == COLLECT_PAYMENT_REFS_REQUEST) {
            Log.d(TAG, "onActivityResult: Payment with References");
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);
                    if (payment != null && payment.getReferences() != null) {
                        Log.d(TAG, payment.getReferences().toString());
                        if (payment.getReferences().get(0) != null)
                            payWithRefsResult.setText(payment.getReferences().get(0).toString());
                    }
                }
            }

        } else if (requestCode == COLLECT_PAYMENT_WITH_TIP_REQUEST) {
            Log.d(TAG, "onActivityResult: Payment with Tip Amount");
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);
                    if (payment != null) {
                        if (payment.getTipAmounts() != null && payment.getTransactions().get(0) != null) {
                            Map tipMap = payment.getTipAmounts();
                            Log.d(TAG, "TIP_MAP: " + tipMap.toString());
                            Log.d(TAG, "TIP_AMOUNT: " + payment.getTipAmount());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tipStatus.setText("Tip Returned");
                                    }
                                });
                        }else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tipStatus.setText("Tip Not Returned");
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    /**
     * pulls transaction Ids by referenceId from the content provider
     */
    private void getTransactionFromContentProvider() {
        ContentResolver resolver = getContentResolver();
        String[] projection = new String[]{TransactionreferencesColumns.TRANSACTIONID};
        Cursor cursor = resolver.query(TransactionreferencesColumns.CONTENT_URI,
                projection,
                TransactionreferencesColumns.REFERENCEID + " = ?",
                new String[]{lastReferenceId},
                null);
        List<String> transactions = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                transactions.add(cursor.getString(0));
            }
        }

        cursor.close();
        // handle transactions
        // full transaction can get retrieved using IPoyntTransactionService.getTransaction
        if (!transactions.isEmpty()) {
            logData("Found the following transactions for referenceId " + lastReferenceId + ": ");
            for (String txnId : transactions) {
                logData(txnId);
            }
        } else {
            logData("No Transactions found");
        }
    }

    public void logData(final String data) {
        Log.d(TAG, data);
    }

}
