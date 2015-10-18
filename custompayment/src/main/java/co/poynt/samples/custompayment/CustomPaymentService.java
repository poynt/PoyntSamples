package co.poynt.samples.custompayment;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Calendar;
import java.util.HashMap;

import co.poynt.api.model.CustomFundingSource;
import co.poynt.api.model.FundingSource;
import co.poynt.api.model.FundingSourceType;
import co.poynt.api.model.ProcessorResponse;
import co.poynt.api.model.ProcessorStatus;
import co.poynt.api.model.ProviderVerification;
import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionAction;
import co.poynt.api.model.TransactionAmounts;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntCustomTransactionService;
import co.poynt.os.services.v1.IPoyntCustomTransactionServiceListener;
import timber.log.Timber;

/**
 * This class implements sample custom payment service that show how to
 * support custom payment method like gift card in Poynt OS.
 */
public class CustomPaymentService extends Service {

    private HashMap<String, Transaction> inMemoryAuthTransactions = new HashMap<String, Transaction>();
    private HashMap<String, Transaction> inMemoryCapturedTransactions = new HashMap<String, Transaction>();

    /**
     * Keys are obtained from Poynt developer portal while signing up for updloading a
     * new APK.
     */
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public CustomPaymentService() {
        Timber.plant(new Timber.DebugTree() {
            // Add the line number to the tag.
            @Override
            protected String createStackElementTag(StackTraceElement element) {
                return super.createStackElementTag(element) + ':' + element.getLineNumber();
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // IMPORTANT: For sample app key is stored in assets folder.
        // But in real custom payment app key should be store in backend server and
        // should be fetched when this app start up.
        // This KeyPair is the same key issued by Poynt website when you register the app
        // with Poynt.
        KeyPair keyPair = KeyUtils.getKeyPair(this, "key");
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IPoyntCustomTransactionService.Stub mBinder = new IPoyntCustomTransactionService.Stub() {

        @Override
        public void authorizeTransaction(String requestId,
                                         Transaction transaction,
                                         IPoyntCustomTransactionServiceListener IPoyntCustomTransactionServiceListener) throws RemoteException {
            Timber.d("authorizeTransaction RequestId (%s)", requestId);
            transaction.setId(TimeBasedUUIDGenerator.generateId());
            new AuthorizeTransactionTask(requestId, IPoyntCustomTransactionServiceListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, transaction);
        }

        @Override
        public void voidTransaction(String requestId, String transactionId,
                                    IPoyntCustomTransactionServiceListener IPoyntCustomTransactionServiceListener) throws RemoteException {
            Timber.d("voidTransaction RequestId (%s)", requestId);
            new VoidTransactionTask(requestId, IPoyntCustomTransactionServiceListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, transactionId);
        }

        @Override
        public void captureTransaction(String requestId, String transactionId,
                                       IPoyntCustomTransactionServiceListener IPoyntCustomTransactionServiceListener) throws RemoteException {
            Timber.d("captureTransaction RequestId (%s)", requestId);
            new CaptureTransactionTask(requestId, IPoyntCustomTransactionServiceListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, transactionId);
        }

        @Override
        public void refundTransaction(
                String requestId,
                Transaction transaction,
                IPoyntCustomTransactionServiceListener IPoyntCustomTransactionServiceListener) throws RemoteException {
            Timber.d("refundTransaction RequestId (%s)", requestId);
            new RefundTransactionTask(requestId, IPoyntCustomTransactionServiceListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, transaction);
        }

        @Override
        public void getTransaction(String requestId, String transactionId, IPoyntCustomTransactionServiceListener IPoyntCustomTransactionServiceListener) throws RemoteException {
            Timber.d("getTransaction RequestId (%s)", requestId);
            new GetTransactionTask(requestId, IPoyntCustomTransactionServiceListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, transactionId);
        }
    };

    private void addProcessorResponse(Transaction transaction, ProcessorStatus status) {
        long totalAmount = 0l;
        TransactionAmounts amount = transaction.getAmounts();
        totalAmount += amount.getTransactionAmount();
        totalAmount += amount.getTipAmount();
        String message = transaction.getId() + String.valueOf(totalAmount);
        String signature = KeyUtils.signMessage(message, privateKey);
        // set the signature to the transaction.
        ProcessorResponse processorResponse = new ProcessorResponse();
        processorResponse.setApprovedAmount(transaction.getAmounts().getOrderAmount());
        processorResponse.setTransactionId(transaction.getId().toString());
        processorResponse.setStatus(status);
        //NOTE: Even though Status code is a string, we expect to pass an integer code to it.
        processorResponse.setStatusCode("1");

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] thedigest = md.digest(publicKey.getEncoded());
        String keyHash = Base64.encodeToString(thedigest, Base64.DEFAULT);
        ProviderVerification verification = new ProviderVerification();
        verification.setPublicKeyHash(keyHash);
        verification.setSignature(signature);
        processorResponse.setProviderVerification(verification);
        transaction.setProcessorResponse(processorResponse);
    }

    public class AuthorizeTransactionTask extends AsyncTask<Transaction, Void, Transaction> {

        private IPoyntCustomTransactionServiceListener callback;
        private String requestId;

        public AuthorizeTransactionTask(String requestId, IPoyntCustomTransactionServiceListener callback) {
            this.callback = callback;
            this.requestId = requestId;
            Timber.d("AuthorizeTransaction for requestId (%s)", this.requestId);
        }

        @Override
        protected Transaction doInBackground(Transaction... params) {
            Transaction transaction = params[0];
            FundingSource fundingSource = transaction.getFundingSource();
            if (fundingSource != null) {
                CustomFundingSource customFundingSource = fundingSource.getCustomFundingSource();
                // If funding source is null, that means we need to launch
                // a activity to get card info filled in by merchant.
                if (customFundingSource != null
                        && fundingSource.getType() == FundingSourceType.CUSTOM_FUNDING_SOURCE) {
                    if (fundingSource.getCard() != null) {
                        transaction.setStatus(TransactionStatus.AUTHORIZED);
                        // Read the card and
                        // set customer account id. This is required to authorize the transaction.
                        if (fundingSource.getCard().getNumber() != null) {
                            customFundingSource.setAccountId(fundingSource.getCard().getNumber());
                        } else if (fundingSource.getCard().getKeySerialNumber() != null) {
                            customFundingSource.setAccountId(fundingSource.getCard().getKeySerialNumber());
                        } else {
                            if (fundingSource.getCard().getTrack1data() != null) {
                                customFundingSource.setAccountId(fundingSource.getCard().getTrack1data());
                            } else if (fundingSource.getCard().getTrack2data() != null) {
                                customFundingSource.setAccountId(fundingSource.getCard().getTrack2data());
                            }
                        }
                        fundingSource.setCustomFundingSource(customFundingSource);
                        transaction.setFundingSource(fundingSource);

                        // add processor response to the transaction by putting providersignature.
                        addProcessorResponse(transaction, ProcessorStatus.Successful);
                        Timber.d("Creating Transaction : %s", transaction.getAction());
                        inMemoryAuthTransactions.put(transaction.getId().toString(), transaction);
                    }
                    return transaction;
                } else {
                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Transaction transaction) {
            super.onPostExecute(transaction);
            try {
                if (transaction != null) {
                    if (transaction.getFundingSource() != null) {
                        FundingSource fundingSource = transaction.getFundingSource();
                        CustomFundingSource customFundingSource = fundingSource.getCustomFundingSource();
                        if (customFundingSource == null) {
                            PoyntError poyntError = new PoyntError(PoyntError.CODE_UNAUTHORIZED);
                            poyntError.setReason(" Failed to Authorize transaction");
                            Timber.e("Error received (%s)", poyntError.toString());
                            callback.onError(requestId, poyntError);
                        }
                        if (customFundingSource.getAccountId() == null) {
                            // prepare intent to launch activity.
                            // make sure not to launch a full screen activity.
                            //
                            Intent intent = new Intent(CustomPaymentService.this, CardEntryActivity.class);
                            intent.putExtra("transaction", transaction);
                            // start the activity.
                            callback.onLaunchActivity(intent, requestId);
                        } else {
                            Timber.d("processed transaction (%s)", (transaction != null ? transaction.getId() : "-failed-"));
                            callback.onSuccess(requestId, "Processed", transaction);
                        }

                    } else {
                        Timber.d("processed transaction (%s)", (transaction != null ? transaction.getId() : "-failed-"));
                        callback.onSuccess(requestId, "Processed", transaction);
                    }
                } else {
                    PoyntError poyntError = new PoyntError(PoyntError.CODE_UNAUTHORIZED);
                    poyntError.setReason(" Failed to Authorize transaction");
                    Timber.e("Error received (%s)", poyntError.toString());
                    callback.onError(requestId, poyntError);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    public class CaptureTransactionTask extends AsyncTask<String, Void, Transaction> {

        private IPoyntCustomTransactionServiceListener callback;
        private String requestId;
        private PoyntError poyntError;

        public CaptureTransactionTask(String requestId, IPoyntCustomTransactionServiceListener callback) {
            this.callback = callback;
            this.requestId = requestId;
            Timber.d("Capture Transaction for requestId (%s)", this.requestId);
        }

        @Override
        protected Transaction doInBackground(String... params) {
            String transactionId = params[0];
            Transaction transaction = inMemoryAuthTransactions.get(transactionId);
            if (transaction != null) {
                // change the Auth transaction to captured.
                transaction.setStatus(TransactionStatus.CAPTURED);

                Timber.d("Capturing Transaction : %s", transactionId);
                // create a new captured object and set the action as captured transaction.
                // This object will be returned as a child object of original auth transaction.
                // For on Auth transaction there can be many Captured transaction.
                Transaction capturedTransaction = new Transaction(false,
                        false,false,
                        Calendar.getInstance(), Calendar.getInstance(),
                        transaction.getContext(), transaction.getFundingSource(), transaction.getLinks(),
                        transaction.getReferences(), transaction.get_id(), transaction.get_id(),
                        transaction.getCustomerUserId(), transaction.getProcessorResponse(),
                        transaction.getTransactionNumber(),
                        transaction.getReceiptEmailAddress(),
                        transaction.getNotes(),
                        transaction.getApprovalCode(),
                        TransactionAction.CAPTURE,
                        transaction.getAmounts(),
                        TransactionStatus.CAPTURED,
                        TimeBasedUUIDGenerator.generateId(),
                        transaction.getId(),
                        null);

                addProcessorResponse(capturedTransaction, ProcessorStatus.Successful);

                inMemoryCapturedTransactions.put(capturedTransaction.getId().toString(), capturedTransaction);
                return capturedTransaction;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Transaction transaction) {
            super.onPostExecute(transaction);
            try {
                if (transaction != null) {
                    Timber.d("Captured transaction (%s)", (transaction != null ? transaction.getId() : "-failed-"));
                    callback.onSuccess(requestId, "Captured", transaction);
                } else if (poyntError != null) {
                    PoyntError poyntError = new PoyntError(PoyntError.CODE_UNAUTHORIZED);
                    poyntError.setReason(" Failed to Capture transaction");
                    Timber.e("Error received (%s)", poyntError.toString());
                    callback.onError(requestId, poyntError);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public class RefundTransactionTask extends AsyncTask<Transaction, Void, Transaction> {

        private IPoyntCustomTransactionServiceListener callback;
        private String requestId;

        public RefundTransactionTask(String requestId, IPoyntCustomTransactionServiceListener callback) {
            this.callback = callback;
            this.requestId = requestId;
            Timber.d("AuthorizeTransaction for requestId (%s)", this.requestId);
        }

        @Override
        protected Transaction doInBackground(Transaction... params) {
            Transaction refundTransaction = params[0];

            Transaction transaction =
                    inMemoryCapturedTransactions.get(refundTransaction.getParentId().toString());
            if (transaction != null) {
                // Do extra processing if needed.
                transaction.setStatus(TransactionStatus.REFUNDED);
                addProcessorResponse(transaction, ProcessorStatus.Successful);
                Timber.d("Creating Transaction : %s", transaction.getAction());
                inMemoryCapturedTransactions.put(transaction.getId().toString(), transaction);
                return transaction;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Transaction transaction) {
            super.onPostExecute(transaction);
            try {
                if (transaction != null) {
                    Timber.d("processed transaction (%s)", (transaction != null ? transaction.getId() : "-failed-"));
                    callback.onSuccess(requestId, "Refunded", transaction);
                } else {
                    PoyntError poyntError = new PoyntError(PoyntError.CODE_UNAUTHORIZED);
                    poyntError.setReason(" Failed to Authorize transaction");
                    Timber.e("Error received (%s)", poyntError.toString());
                    callback.onError(requestId, poyntError);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public class GetTransactionTask extends AsyncTask<String, Void, Transaction> {

        private IPoyntCustomTransactionServiceListener callback;
        private String requestId;
        private PoyntError poyntError;

        public GetTransactionTask(String requestId, IPoyntCustomTransactionServiceListener callback) {
            this.callback = callback;
            this.requestId = requestId;
            Timber.d("Capture Transaction for requestId (%s)", this.requestId);
        }

        @Override
        protected Transaction doInBackground(String... params) {
            String transactionId = params[0];

            Transaction transaction = inMemoryAuthTransactions.get(transactionId);
            if (transaction != null) {
                addProcessorResponse(transaction, ProcessorStatus.Successful);
            }
            return transaction;
        }

        @Override
        protected void onPostExecute(Transaction transaction) {
            super.onPostExecute(transaction);
            try {
                if (transaction != null) {
                    Timber.d("Get transaction (%s)", (transaction != null ? transaction.getId() : "-failed-"));
                    callback.onSuccess(requestId, "get", transaction);
                } else if (poyntError != null) {
                    PoyntError poyntError = new PoyntError(PoyntError.CODE_UNAUTHORIZED);
                    poyntError.setReason(" Failed to get transaction");
                    Timber.e("Error received (%s)", poyntError.toString());
                    callback.onError(requestId, poyntError);
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public class VoidTransactionTask extends AsyncTask<String, Void, Transaction> {

        private IPoyntCustomTransactionServiceListener callback;
        private String requestId;
        private PoyntError poyntError;

        public VoidTransactionTask(String requestId, IPoyntCustomTransactionServiceListener callback) {
            this.callback = callback;
            this.requestId = requestId;
            Timber.d("Void Transaction for requestId (%s)", this.requestId);
        }

        @Override
        protected Transaction doInBackground(String... params) {
            String transactionId = params[0];
            Transaction transaction = inMemoryAuthTransactions.get(transactionId);
            if (transaction != null && transaction.getStatus() != TransactionStatus.AUTHORIZED) {
                Timber.d(" Cannot void captured Transaction for requestId (%s)", this.requestId);
                return null;
            }
            transaction.setStatus(TransactionStatus.VOIDED);
            addProcessorResponse(transaction, ProcessorStatus.Successful);
            return transaction;
        }

        @Override
        protected void onPostExecute(Transaction transaction) {
            super.onPostExecute(transaction);
            try {
                if (transaction != null) {
                    Timber.d("Void transaction (%s)", (transaction != null ? transaction.getId() : "-failed-"));
                    callback.onSuccess(requestId, "Voided", transaction);
                } else if (poyntError != null) {
                    PoyntError poyntError = new PoyntError(PoyntError.CODE_UNAUTHORIZED);
                    poyntError.setReason(" Failed to get transaction");
                    Timber.e("Error received (%s)", poyntError.toString());
                    callback.onError(requestId, poyntError);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}