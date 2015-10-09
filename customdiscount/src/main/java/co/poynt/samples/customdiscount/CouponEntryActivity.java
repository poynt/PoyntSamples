package co.poynt.samples.customdiscount;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import co.poynt.api.model.Order;
import co.poynt.api.model.Transaction;
import co.poynt.os.model.DiscountStatus;
import co.poynt.os.services.v1.IPoyntCustomDiscountService;
import co.poynt.os.services.v1.IPoyntCustomDiscountServiceListener;
import timber.log.Timber;

/**
 * Created by sathyaiyer on 9/29/15.
 */
public class CouponEntryActivity extends Activity {

    private Order order = null;
    Button cancelButton;
    Button doneButton;
    EditText couponInfo;
    Intent originalIntent = null;
    Intent resultIntent = null;

    private IPoyntCustomDiscountService CouponService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        originalIntent = getIntent();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.poynt_coupon);

        if (originalIntent != null) {
            order = originalIntent.getParcelableExtra("order");
            if (order == null) {
                Timber.e(" Null order passed");
            }
        }
        doneButton = (Button) findViewById(R.id.yes);
        couponInfo = (EditText) findViewById(R.id.couponcode);

        if (order != null) {
            doneButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            validateCouponInfo();
                            sendResult();
                        }
                    }
            );
            Intent serviceIntent = new Intent();
            serviceIntent.setClass(this, CustomCouponService.class);
            bindService(serviceIntent,
                    serviceConnection,
                    Context.BIND_AUTO_CREATE);
        } else {
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


    private boolean validateCouponInfo() {
        String cardEntered = couponInfo.getText().toString();
        if (cardEntered.length() < 4) {
            return false;
        }
        return true;
    }

    private void sendResult() {
        String couponCode = couponInfo.getText().toString();
        if (couponCode != null && couponCode.length() > 4) {
            resultIntent = new Intent();
            resultIntent.putExtra("order", order);
            // get the transaction authorized.
            // for sample sake we are calling the same service but this is where you call you own backend
            String requestId = UUID.randomUUID().toString();
            try {
                CouponService.applyDiscount(
                        requestId,
                        order,
                        null,
                        couponCode,
                        discountServiceListener);
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
        if (CouponService != null) {
            unbindService(serviceConnection);
        }
    }

    private void finishWithError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultIntent = new Intent();
                resultIntent.putExtra("order", order);
                setResult(RESULT_CANCELED, resultIntent);
                finish();
            }
        });
    }

    private void finishWithSuccess(final Order order) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultIntent = new Intent();
                resultIntent.putExtra("order", order);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }


    private IPoyntCustomDiscountServiceListener discountServiceListener = new IPoyntCustomDiscountServiceListener.Stub() {
        @Override
        public void onResponse(DiscountStatus discountStatus, Transaction transaction, Order order, String s) throws RemoteException {
            if (discountStatus.getCode() == DiscountStatus.Code.SUCCESS) {
                finishWithSuccess(order);
            } else {
                finishWithError();
            }
        }

        @Override
        public void onLaunchActivity(Intent intent, String s) throws RemoteException {
            finishWithError();
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CouponService = IPoyntCustomDiscountService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            CouponService = null;
        }
    };
}
