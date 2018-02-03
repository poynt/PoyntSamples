package co.poynt.samplegiftcardprocessor;


import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import co.poynt.api.model.AdjustTransactionRequest;
import co.poynt.api.model.BalanceInquiry;
import co.poynt.api.model.EMVData;
import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionAction;
import co.poynt.os.model.Payment;
import co.poynt.os.services.v1.IPoyntCheckCardListener;
import co.poynt.os.services.v1.IPoyntTransactionBalanceInquiryListener;
import co.poynt.os.services.v1.IPoyntTransactionService;
import co.poynt.os.services.v1.IPoyntTransactionServiceListener;
import co.poynt.samplegiftcardprocessor.core.TransactionManager;

public class SampleGiftCardTransactionProcessorService extends Service {

    private static final String TAG = "SampleGiftCardProcessor";

    public SampleGiftCardTransactionProcessorService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IPoyntTransactionService.Stub mBinder = new IPoyntTransactionService.Stub() {

        @Override
        public void createTransaction(Transaction transaction, String requestId, IPoyntTransactionServiceListener listener) throws RemoteException {
            Log.d(TAG, "createTransaction :" + requestId);
            // DO NOT USE
        }

        @Override
        public void processTransaction(final Transaction transaction, final String requestId, final IPoyntTransactionServiceListener listener) throws RemoteException {
            Log.d(TAG, "processTransaction :" + requestId);

            final TransactionManager transactionManager = SampleGiftcardTransactionProcessorApplication.getInstance().getTransactionManager();

            if (transaction != null && transaction.getAction() != TransactionAction.REFUND &&
                    transaction.getFundingSource().getCard() == null) {
                // The payment was initiated using the GIFT CARD button in payment options
                Intent paymentActivity = new Intent("COLLECT_CUSTOM_PAYMENT");
                paymentActivity.setComponent(new ComponentName(getPackageName(), PaymentActivity.class.getName()));
                paymentActivity.putExtra("transaction", transaction);
                listener.onLaunchActivity(paymentActivity, requestId);
            } else {
                // otherwise this is a SALE via card swipe or a REFUND
                transactionManager.processTransaction(transaction, requestId, listener);
            }
        }


        @Override
        public void voidTransaction(String transactionId, EMVData emvData, String requestId, IPoyntTransactionServiceListener listener) throws RemoteException {
            Log.d(TAG, "voidTransaction :" + requestId);
            TransactionManager transactionManager =
                    SampleGiftcardTransactionProcessorApplication.getInstance().getTransactionManager();
            transactionManager.voidTransaction(transactionId, emvData, requestId, listener);

//            PoyntError poyntError = new PoyntError();
//            //poyntError.setCode(PoyntError.SILENT_CANCEL);
//            poyntError.setApiErrorCode(Code.PROCESSOR_DECLINED);
//            poyntError.setReason("processor declined");
//            try {
//                listener.onResponse(null, requestId, poyntError);
//            } catch (RemoteException e1) {
//                e1.printStackTrace();
//            }

        }

        @Override
        public void captureTransaction(String transactionId,
                                       AdjustTransactionRequest transaction,
                                       String requestId,
                                       IPoyntTransactionServiceListener listener) throws RemoteException {
            Log.d(TAG, "captureTransaction:" + requestId);
            TransactionManager transactionManager = SampleGiftcardTransactionProcessorApplication.getInstance().getTransactionManager();
            transactionManager.captureTransaction(transactionId,
                    transaction, requestId, listener);

        }

        @Override
        public void captureAllTransactions(String requestId) throws RemoteException {
            Log.d(TAG, "captureAllTransactions w/ RequestId:" + requestId);

        }

        @Override
        public void updateTransaction(String transactionId, AdjustTransactionRequest transaction, String requestId, IPoyntTransactionServiceListener listener) throws RemoteException {
            Log.d(TAG, "updateTransaction:" + requestId);
            TransactionManager transactionManager = SampleGiftcardTransactionProcessorApplication.getInstance().getTransactionManager();
            transactionManager.updateTransaction(transactionId, transaction, requestId, listener);
        }

        /**
         * Doesn't apply for non-EMV transactions
         * @param transactionId
         * @param emvData
         * @param requestId
         * @throws RemoteException
         */
        @Override
        public void captureEmvData(String transactionId, EMVData emvData, String requestId) throws RemoteException {
            //no-op
        }

        @Override
        public void getBalanceInquiry(BalanceInquiry balanceInquiry, String s, IPoyntTransactionBalanceInquiryListener iPoyntTransactionBalanceInquiryListener) throws RemoteException {

        }

        @Override
        public void reverseTransaction(String originalRequestId, String originalTransactionId, EMVData emvData, String requestId) throws RemoteException {
            Log.d(TAG, "reverseTransaction:" + originalRequestId);
            // reverse transaction - eg. timeout reversal
        }

        @Override
        public void getTransaction(String transactionId, String requestId, IPoyntTransactionServiceListener listener) throws RemoteException {
            Log.d(TAG, "getTransaction:" + transactionId);
            TransactionManager transactionManager = SampleGiftcardTransactionProcessorApplication.getInstance().getTransactionManager();
            transactionManager.getTransaction(transactionId, requestId, listener);
        }

        @Override
        public void saveTransaction(Transaction transaction, String requestId) throws RemoteException {
            Log.d(TAG, "saveTransaction:" + transaction.toString());
        }


        @Override
        public void checkCard(Payment payment, String serviceCode, String cardHolderName,
                              String expirationDate, String last4, String binRange, String AID,
                              String applicationLabel, String panSequenceNumber,
                              String issuerCountryCode, String encryptedPAN,
                              String encryptedTrack2, int issuerCodeTableIndex, String applicationPreferredName,
                              String keyIdentifier, String appCurrencyCode, IPoyntCheckCardListener callback)
                throws RemoteException {
            Log.d(TAG, "checkCard called");
            // no op - not applicable for gift cards
        }

    };
}
