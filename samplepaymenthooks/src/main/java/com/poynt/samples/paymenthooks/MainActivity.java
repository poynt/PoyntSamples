package com.poynt.samples.paymenthooks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import co.poynt.api.model.CustomFundingSource;
import co.poynt.api.model.CustomFundingSourceType;
import co.poynt.api.model.FundingSource;
import co.poynt.api.model.FundingSourceType;
import co.poynt.api.model.Order;
import co.poynt.api.model.OrderItem;
import co.poynt.api.model.OrderItemStatus;
import co.poynt.api.model.Processor;
import co.poynt.api.model.ProcessorResponse;
import co.poynt.api.model.ProcessorStatus;
import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionAction;
import co.poynt.api.model.TransactionAmounts;
import co.poynt.api.model.TransactionReference;
import co.poynt.api.model.TransactionReferenceType;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.Constants;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Payment payment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button setNewAmount = (Button) findViewById(R.id.set1usd);
        setNewAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payment.setAmount(payment.getAmount() / 2);

                Intent result = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK_RESULT);
                result.putExtra(Constants.HooksExtras.PAYMENT, payment);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

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

        Button closeNoPayment = (Button) findViewById(R.id.closeNoPayment);
        closeNoPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent result = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK_RESULT);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

        /** ADD PROMO ITEM TO PAYMENT
         * This method adds a promo item to the order
         * The following may happen depending on the order
         *  - If order exists an order item is added and the amounts are updated
         *  - If order does not exist then the amount needs to be incremented and item info is added to references
         * */
        //TODO: bind to a service to save Order. check other samples for a reference
        Button addTransaction = (Button) findViewById(R.id.addOrder);
        addTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Order order = payment.getOrder();
                if (order != null) {
                    List<OrderItem> orderItems = order.getItems();
                    if (orderItems == null) {
                        orderItems = new ArrayList<OrderItem>();
                    }
                    OrderItem promoItem = new OrderItem();
                    promoItem.setUnitPrice(100L);
                    promoItem.setQuantity(1.0F);
                    promoItem.setName("Fivestars promo");
                    promoItem.setId(123456);
                    promoItem.setStatus(OrderItemStatus.FULFILLED);
                    orderItems.add(promoItem);
                    order.setItems(orderItems);
                    order.getAmounts().setNetTotal(order.getAmounts().getNetTotal() + 100L);
                } else {
                    List<TransactionReference> references = payment.getReferences();
                    if (references == null) {
                        references = new ArrayList<>();
                    }
                    TransactionReference reference = new TransactionReference();
                    reference.setId("promoitemreference1");
                    reference.setCustomType("PROMO_REFERENCE");
                    reference.setType(TransactionReferenceType.CUSTOM);
                    references.add(reference);
                    payment.setReferences(references);
                    payment.setNotes((payment.getNotes() != null ? payment.getNotes() + "\n" : "") + "$1 promo item added");
                }
                payment.setAmount(payment.getAmount() + 100L);
                Intent result = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK_RESULT);
                result.putExtra(Constants.HooksExtras.PAYMENT, payment);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

        /** REDEEM LOYALTY ITEM
         * Redeems a loyalty item
         * A new transaction is created for the redemption item
         * */
        Button add1Transaction = (Button) findViewById(R.id.add1Transaction);
        add1Transaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // why not create an order for loyalty redemption?
                Transaction transaction = new Transaction();

                long amount = payment.getAmount() / 2;

                TransactionAmounts ta = new TransactionAmounts();
                ta.setTransactionAmount(amount);
                ta.setOrderAmount(amount);
                ta.setCurrency("USD");
                transaction.setAmounts(ta);

                CustomFundingSource customFundingSource = new CustomFundingSource();
                customFundingSource.setAccountId("1234567890");
                customFundingSource.setType(CustomFundingSourceType.REWARD);
                customFundingSource.setName("FIVESTARS");
                customFundingSource.setProvider("FIVESTARS");
                customFundingSource.setProcessor("Poynt");

                FundingSource fs = new FundingSource();
                fs.setType(FundingSourceType.CUSTOM_FUNDING_SOURCE);
                fs.setCustomFundingSource(customFundingSource);
                transaction.setFundingSource(fs);

                transaction.setStatus(TransactionStatus.CAPTURED);
                transaction.setAction(TransactionAction.SALE);
                transaction.setSettled(true);

                transaction.setId(UUID.randomUUID());
                transaction.setCreatedAt(Calendar.getInstance());
                transaction.setUpdatedAt(Calendar.getInstance());

                ProcessorResponse processorResponse = new ProcessorResponse();
                processorResponse.setProcessor(Processor.CREDITCALL);
                processorResponse.setAcquirer(Processor.CHASE_PAYMENTECH);
                processorResponse.setStatus(ProcessorStatus.Successful);
                processorResponse.setApprovalCode("123456");
                processorResponse.setStatusCode("200");
                processorResponse.setApprovedAmount(transaction.getAmounts().getTransactionAmount());
                processorResponse.setStatusMessage("Approved");

                transaction.setProcessorResponse(processorResponse);

                payment.setTransactions(Collections.singletonList(transaction));
                Intent result = new Intent(Intents.ACTION_PROCESS_PAYMENT_HOOK_RESULT);
                result.putExtra(Constants.HooksExtras.PAYMENT, payment);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

        Button addExactTransaction = (Button) findViewById(R.id.addExactTransaction);
        addExactTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // why not create an order for loyalty redemption?
                Transaction transaction = new Transaction();

                TransactionAmounts ta = new TransactionAmounts();
                ta.setTransactionAmount(payment.getAmount());
                ta.setOrderAmount(payment.getAmount());
                ta.setCurrency("USD");
                transaction.setAmounts(ta);

                CustomFundingSource customFundingSource = new CustomFundingSource();
                customFundingSource.setAccountId("1234567890");
                customFundingSource.setType(CustomFundingSourceType.OTHER);
                customFundingSource.setName("FIVESTARS");
                customFundingSource.setProvider("poynt");
                customFundingSource.setProcessor("Poynt");

                FundingSource fs = new FundingSource();
                fs.setType(FundingSourceType.CUSTOM_FUNDING_SOURCE);
                fs.setCustomFundingSource(customFundingSource);
                transaction.setFundingSource(fs);

                transaction.setStatus(TransactionStatus.CAPTURED);
                transaction.setAction(TransactionAction.SALE);
                transaction.setSettled(true);

                transaction.setId(UUID.randomUUID());
                transaction.setCreatedAt(Calendar.getInstance());
                transaction.setUpdatedAt(Calendar.getInstance());

                ProcessorResponse processorResponse = new ProcessorResponse();
                processorResponse.setProcessor(Processor.CREDITCALL);
                processorResponse.setAcquirer(Processor.CHASE_PAYMENTECH);
                processorResponse.setStatus(ProcessorStatus.Successful);
                processorResponse.setApprovalCode("123456");
                processorResponse.setStatusCode("200");
                processorResponse.setApprovedAmount(transaction.getAmounts().getTransactionAmount());
                processorResponse.setStatusMessage("Approved");

                transaction.setProcessorResponse(processorResponse);

                payment.setTransactions(Collections.singletonList(transaction));

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
