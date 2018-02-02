package com.sampleloyaltyapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import co.poynt.api.model.Discount;
import co.poynt.api.model.OrderStatus;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    Payment payment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button addDiscount = (Button) findViewById(R.id.addDiscount);
        addDiscount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long discountAmount = 100l;
                List<Discount> discounts = payment.getOrder().getDiscounts();
                if (discounts == null) {
                    discounts = new ArrayList<>();
                }
                Discount discount = new Discount();
                // add one dollar discount at order level
                // NOTE: discount amount is always negative
                discount.setAmount(-1l * discountAmount);
                discount.setCustomName("Loyalty Discount");
                discount.setProcessor(getPackageName());
                discounts.add(discount);
                payment.getOrder().setDiscounts(discounts);
                // discount total should be updated
                long discountTotal = payment.getOrder().getAmounts().getDiscountTotal() + discount.getAmount();
                payment.getOrder().getAmounts().setDiscountTotal(discountTotal);
                // update the over all total
                long orderTotal = payment.getOrder().getAmounts().getNetTotal();
                if (orderTotal >= discountAmount) {
                    payment.getOrder().getAmounts().setNetTotal(orderTotal - discountAmount);
                    payment.setAmount(orderTotal - discountAmount);
                    if (orderTotal == discountAmount){
                        payment.getOrder().getStatuses().setStatus(OrderStatus.COMPLETED);
                    }
                    Log.d(TAG, "Discount added to order: " + payment);
                    Intent result = new Intent(Intents.ACTION_PROCESS_LOYALTY_RESULT);
                    result.putExtra("payment", payment);
                    setResult(Activity.RESULT_OK, result);
                    finish();
                }else{
                    Toast.makeText(MainActivity.this, "Discount amount is larger than total", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                }
            }
        });

        // Get the intent that started this activity
        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
        } else {
            Log.e(TAG, "Loyalty activity launched with no intent!");
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

        // If the customer is not checked in the activity can call IPoyntSecondScreenService
        // to display collect phone number, email or scan QR code screen to allow customer
        // to check in.
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        if (Intents.ACTION_PROCESS_LOYALTY.equals(action)) {
            payment = intent.getParcelableExtra("payment");
            if (payment == null) {
                Log.e(TAG, "launched with no payment object");
                Intent result = new Intent(Intents.ACTION_PROCESS_LOYALTY_RESULT);
                setResult(Activity.RESULT_CANCELED, result);
                finish();
            } else {
                // add a discount to the order
                if (payment.getOrder() == null) {
                    Log.e(TAG, "launched with no order object");
                    Intent result = new Intent(Intents.ACTION_PROCESS_LOYALTY_RESULT);
                    setResult(Activity.RESULT_CANCELED, result);
                    finish();
                } else {
                    // wait till the Add discount is clicked
                }
            }
        } else {
            Log.e(TAG, "launched with unknown intent action");
            Intent result = new Intent(Intents.ACTION_PROCESS_LOYALTY_RESULT);
            setResult(Activity.RESULT_CANCELED, result);
            finish();
        }
    }
}
