package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntInAppBillingService;
import co.poynt.os.services.v1.IPoyntInAppBillingServiceListener;


public class InAppBillingActivity extends Activity {

    private static final String TAG = InAppBillingActivity.class.getSimpleName();
    private TextView resultText;

    IPoyntInAppBillingService mBillingService;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBillingService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mBillingService = IPoyntInAppBillingService.Stub.asInterface(service);

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_billing);
        resultText = (TextView) findViewById(R.id.resultText);
        ActionBar actionBar = getActionBar();
        if (actionBar !=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

//        Intent serviceIntent =
//                Intents.getComponentIntent(
//                        new ComponentName("co.poynt.services", "co.poynt.os.services.v1.IPoyntInAppBillingService"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent();
        serviceIntent.setClassName(IPoyntInAppBillingService.class.getPackage().getName(), IPoyntInAppBillingService.class.getName());
        //serviceIntent.setAction(IPoyntInAppBillingService.class.getName());
        //serviceIntent.setPackage("co.poynt.os.services.v1");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id==android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onResume(){
        super.onResume();
        if (mBillingService != null) {
            checkSubscriptionStatus();
        }else{
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkSubscriptionStatus();
                }
            }, 1000L);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBillingService != null) {
            unbindService(mServiceConn);
        }
    }



    private void checkSubscriptionStatus(){
        try {
            if (mBillingService != null) {
                String requestId = UUID.randomUUID().toString();
                mBillingService.getSubscriptions("co.poynt.samples.codesamples", requestId,
                        new IPoyntInAppBillingServiceListener.Stub(){

                            @Override
                            public void onResponse(String resultJson, PoyntError poyntError, String requestId)
                                    throws RemoteException {
                                if (poyntError != null) {
                                    Toast.makeText(InAppBillingActivity.this, resultJson, Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(InAppBillingActivity.this, poyntError.toString(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });

            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
