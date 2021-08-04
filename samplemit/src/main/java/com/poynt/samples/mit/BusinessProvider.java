package com.poynt.samples.mit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import co.poynt.os.model.Intents;
import co.poynt.os.services.v1.IPoyntBusinessReadListener;
import co.poynt.os.services.v1.IPoyntBusinessService;

public class BusinessProvider {

    public static final String POYNT_SERVICES_PKG = "co.poynt.services";

    private Intent mIntent;
    private Context mContext;
    private IPoyntBusinessService mPoyntBusinessService;

    public BusinessProvider(Context context) {
        this.mContext = context;
        bindService();
    }


    private synchronized void bindService() {
        if (mPoyntBusinessService == null) {
            mContext.bindService(
                    Intents.getComponentIntent(Intents.COMPONENT_POYNT_BUSINESS_SERVICE),
                    businessConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    private void checkService() {
        if (mPoyntBusinessService == null) {
            bindService();
        }
    }

    public void getBusiness(final IPoyntBusinessReadListener listener) {
        checkService();
        if (mPoyntBusinessService != null) {
            try {
                //Timber.d("making remote Business Service call to obtain business");
                mPoyntBusinessService.getBusiness(listener);
            } catch (RemoteException e) {
               // Timber.e("Failed to retrieve business from Poynt Business Service");
            }
        } else {
            // wait ?
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getBusiness(listener);
                }
            }, 1000);
        }
    }

    private ServiceConnection businessConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //Timber.d("BusinessService is connected.");
            mPoyntBusinessService = IPoyntBusinessService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
           // Timber.d("BusinessService is disconnected.");
            mPoyntBusinessService = null;
            // reconnect
            //TODO
        }
    };



    public void finish() {
        try {
            //Timber.d("finishing up business provider");
            mContext.unbindService(businessConnection);
        } catch (Exception e) {
           // Timber.e(e, e.getMessage());
        }
    }
}