package co.poynt.samplegiftcardprocessor;

import android.app.Application;

import co.poynt.samplegiftcardprocessor.core.TransactionManager;


/**
 * Created by palavilli on 1/25/16.
 */
public class SampleGiftcardTransactionProcessorApplication extends Application {
    public static SampleGiftcardTransactionProcessorApplication instance;

    public static SampleGiftcardTransactionProcessorApplication getInstance() {
        return instance;
    }

    TransactionManager transactionManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        transactionManager = TransactionManager.getInstance(this);
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
}
