package co.poynt.samplegiftcardprocessor.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import co.poynt.api.model.AdjustTransactionRequest;
import co.poynt.api.model.CustomFundingSource;
import co.poynt.api.model.CustomFundingSourceType;
import co.poynt.api.model.EMVData;
import co.poynt.api.model.FundingSourceType;
import co.poynt.api.model.Processor;
import co.poynt.api.model.ProcessorResponse;
import co.poynt.api.model.ProcessorStatus;
import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionAction;
import co.poynt.api.model.TransactionAmounts;
import co.poynt.api.model.TransactionReference;
import co.poynt.api.model.TransactionReferenceType;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntSecurityService;
import co.poynt.os.services.v1.IPoyntTransactionServiceListener;

/**
 * Created by palavilli on 11/29/15.
 */
public class TransactionManager {

    private static final String TAG = "TransactionManager";
    private static TransactionManager transactionManager;
    private IPoyntSecurityService poyntSecurityService;
    private Context context;
    private Map<UUID, Transaction> TRANSACTION_CACHE;

    // will use this to print out raw JSON of the Transaction object for debugging purposes
    private Gson gson;
    private Type transactionType;

    private TransactionManager(Context context) {
        this.context = context;
        gson = new Gson();
        transactionType = new TypeToken<Transaction>() {
        }.getType();
        TRANSACTION_CACHE = new HashMap<>();
        bind();
    }

    public static TransactionManager getInstance(Context context) {
        if (transactionManager == null) {
            transactionManager = new TransactionManager(context);
        }
        return transactionManager;
    }

    public synchronized void bind() {
        if (poyntSecurityService == null) {
            context.bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_SECURITY_SERVICE),
                    mConnection, Context.BIND_AUTO_CREATE);
        } else {
            // already connected ?
        }
    }

    public boolean isConnected() {
        if (poyntSecurityService != null) {
            return true;
        } else {
            return false;
        }
    }

    public void shutdown() {
        context.unbindService(mConnection);
    }

    /**
     * Class for interacting with the BusinessService
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.e("TransactionManager", "IPoyntSecurityService is now connected");
            // Following the example above for an AIDL interface,
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            poyntSecurityService = IPoyntSecurityService.Stub.asInterface(service);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e("TransactionManager", "IPoyntSecurityService has unexpectedly disconnected - reconnecting");
            poyntSecurityService = null;
            bind();
        }
    };

    public void processTransaction(final Transaction transaction,
                                   final String requestId,
                                   final IPoyntTransactionServiceListener listener) {
        Log.d(TAG, "PROCESSED TRANSACTION");

        if (transaction != null) {
//            Log.d(TAG, transaction.toString());
            Log.d(TAG, "processTransaction: received " + transaction.getAction() + " request: " +
                    gson.toJson(transaction, transactionType));
        }
        // always make sure we set ID, created_at and updated_at time stamps
        if (transaction.getId() == null) {
            transaction.setId(UUID.randomUUID());
        }
        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(Calendar.getInstance());
        }
        if (transaction.getUpdatedAt() == null) {
            transaction.setUpdatedAt(Calendar.getInstance());
        }


        ProcessorResponse processorResponse = new ProcessorResponse();
        // IMP NOTE: These two MUST match the merchant's settings for now - will be fixed in the next release
        processorResponse.setProcessor(Processor.CREDITCALL);
        processorResponse.setAcquirer(Processor.CHASE_PAYMENTECH);

        // based on action handle the transactions
        if (transaction.getAction() == TransactionAction.AUTHORIZE
                || transaction.getAction() == TransactionAction.SALE) {

            transaction.setStatus(TransactionStatus.CAPTURED);

            // generating random processor transaction id
            setProcessorTransactionId(transaction, processorResponse);

            transaction.getFundingSource().setType(FundingSourceType.CUSTOM_FUNDING_SOURCE);
            CustomFundingSource customFundingSource = transaction.getFundingSource().getCustomFundingSource();
            if (customFundingSource == null) {
                customFundingSource = new CustomFundingSource();
                customFundingSource.setType(CustomFundingSourceType.GIFT_CARD);
            }
            customFundingSource.setName("Starbucks");
            customFundingSource.setAccountId("1234567890");
            customFundingSource.setProcessor("co.poynt.samplegiftcardprocessor");
            customFundingSource.setProvider("Sage");
            customFundingSource.setDescription("Starbucks giftcard");
            transaction.getFundingSource().setCustomFundingSource(customFundingSource);

            processorResponse.setStatus(ProcessorStatus.Successful);
            processorResponse.setApprovalCode("123456");
            if (transaction.getAmounts().getTransactionAmount() == 5555l) {
                processorResponse.setStatus(ProcessorStatus.Successful);
                processorResponse.setApprovedAmount(100l);
                transaction.getAmounts().setTransactionAmount(100l);
                transaction.getAmounts().setOrderAmount(100l);
                transaction.setPartiallyApproved(true);
                processorResponse.setStatusMessage("Partially Approved");
                processorResponse.setStatusCode("300");

            } else {
                processorResponse.setStatusCode("200");
                processorResponse.setApprovedAmount(transaction.getAmounts().getTransactionAmount());
                processorResponse.setStatusMessage("Approved");
//            ProviderVerification providerVerification = new ProviderVerification();
//            providerVerification.setSignature("1234");
//            providerVerification.setPublicKeyHash("ABCD");
//            processorResponse.setProviderVerification(providerVerification);
            }
            if (transaction.getAction() == TransactionAction.AUTHORIZE) {
                transaction.setStatus(TransactionStatus.AUTHORIZED);
            } else {
                transaction.setStatus(TransactionStatus.CAPTURED);
            }
            transaction.setProcessorResponse(processorResponse);

        } else if (transaction.getAction() == TransactionAction.REFUND) {
            // add processor response
            transaction.setStatus(TransactionStatus.REFUNDED);
            processorResponse.setTransactionId(transaction.getId().toString());
            processorResponse.setStatus(ProcessorStatus.Successful);
            processorResponse.setApprovalCode("123456");
            processorResponse.setStatusCode("200");
            processorResponse.setApprovedAmount(transaction.getAmounts().getTransactionAmount());
            processorResponse.setStatusMessage("Approved");
            transaction.setProcessorResponse(processorResponse);

            // also mark the original txn as refunded
            if (transaction.getParentId() != null) {
                Transaction parent = TRANSACTION_CACHE.get(transaction.getParentId());
                parent.setStatus(TransactionStatus.REFUNDED);
                TRANSACTION_CACHE.put(parent.getId(), parent);
            }
        }
        try {
            TRANSACTION_CACHE.put(transaction.getId(), transaction);
            listener.onResponse(transaction, requestId, null);
        } catch (RemoteException e) {
            e.printStackTrace();
            PoyntError poyntError = new PoyntError();
            poyntError.setCode(PoyntError.CARD_DECLINE);
            try {
                listener.onResponse(transaction, requestId, poyntError);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }

    }

    public Transaction processTransaction(Transaction transaction, String customPaymentCode) {
        Log.d(TAG, "PROCESSED TRANSACTION w/ zip code ");

        if (transaction != null) {
            Log.d(TAG, transaction.toString());
        }
        // always make sure we set ID, created_at and updated_at time stamps
        if (transaction.getId() == null) {
            transaction.setId(UUID.randomUUID());
        }
        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(Calendar.getInstance());
        }
        if (transaction.getUpdatedAt() == null) {
            transaction.setUpdatedAt(Calendar.getInstance());
        }


        ProcessorResponse processorResponse = new ProcessorResponse();
        processorResponse.setProcessor(Processor.CREDITCALL);
        processorResponse.setAcquirer(Processor.CHASE_PAYMENTECH);
        // based on action handle the transactions
        if (transaction.getAction() == TransactionAction.AUTHORIZE
                || transaction.getAction() == TransactionAction.SALE) {
            transaction.setStatus(TransactionStatus.CAPTURED);

            setProcessorTransactionId(transaction, processorResponse);

            transaction.getFundingSource().setType(FundingSourceType.CUSTOM_FUNDING_SOURCE);
            CustomFundingSource customFundingSource = transaction.getFundingSource().getCustomFundingSource();
            if (customFundingSource == null) {
                customFundingSource = new CustomFundingSource();
                customFundingSource.setType(CustomFundingSourceType.GIFT_CARD);
            }
            customFundingSource.setName("Starbucks");
            customFundingSource.setAccountId("1234567890");
            customFundingSource.setProcessor("co.poynt.samplegiftcardprocessor");
            customFundingSource.setProvider("Sage");
            customFundingSource.setDescription("Starbucks giftcard");
            transaction.getFundingSource().setCustomFundingSource(customFundingSource);

            processorResponse.setStatus(ProcessorStatus.Successful);
            processorResponse.setApprovalCode("123456");
            if (transaction.getAmounts().getTransactionAmount() == 5555l) {
                processorResponse.setStatus(ProcessorStatus.Successful);
                processorResponse.setApprovedAmount(100l);
                transaction.getAmounts().setTransactionAmount(100l);
                transaction.getAmounts().setOrderAmount(100l);
                processorResponse.setStatusMessage("Partially Approved");
                processorResponse.setStatusCode("300");
            } else {
                processorResponse.setStatusCode("200");
                processorResponse.setApprovedAmount(transaction.getAmounts().getTransactionAmount());
                processorResponse.setStatusMessage("Approved");
//            ProviderVerification providerVerification = new ProviderVerification();
//            providerVerification.setSignature("1234");
//            providerVerification.setPublicKeyHash("ABCD");
//            processorResponse.setProviderVerification(providerVerification);
            }
            if (transaction.getAction() == TransactionAction.AUTHORIZE) {
                transaction.setStatus(TransactionStatus.AUTHORIZED);
            } else {
                transaction.setStatus(TransactionStatus.CAPTURED);
            }
            transaction.setProcessorResponse(processorResponse);

        } else if (transaction.getAction() == TransactionAction.REFUND) {
            // add processor response
            transaction.setStatus(TransactionStatus.REFUNDED);
            processorResponse.setTransactionId(transaction.getId().toString());
            processorResponse.setStatus(ProcessorStatus.Successful);
            processorResponse.setApprovalCode("123456");
            processorResponse.setStatusCode("200");
            processorResponse.setApprovedAmount(transaction.getAmounts().getTransactionAmount());
            processorResponse.setStatusMessage("Approved");
            transaction.setProcessorResponse(processorResponse);
        }

        TRANSACTION_CACHE.put(transaction.getId(), transaction);
        return transaction;
    }

    private void setProcessorTransactionId(Transaction transaction, ProcessorResponse processorResponse) {
        String processorTransactionId = UUID.randomUUID().toString();
        processorResponse.setTransactionId(processorTransactionId);

        // if you need processorTransactionId be returned in the refund request
        // you can add it as a transaction reference
        TransactionReference processorTxnIdReference = new TransactionReference();
        processorTxnIdReference.setType(TransactionReferenceType.CUSTOM);
        processorTxnIdReference.setCustomType("processorTransactionId");
        processorTxnIdReference.setId(processorTransactionId);
        transaction.setReferences(Collections.singletonList(processorTxnIdReference));
    }

    public void captureTransaction(String transactionId, AdjustTransactionRequest adjustTransactionRequest, String requestId, IPoyntTransactionServiceListener listener) {
        Log.d(TAG, "CAPTURED TRANSACTION: " + transactionId);
        // get cached transaction
        Transaction transaction = TRANSACTION_CACHE.get(UUID.fromString(transactionId));
        if (transaction == null) {
            // if we did not find one just generate a new one
            transaction = new Transaction();
            transaction.setId(UUID.fromString(transactionId));
            transaction.setCreatedAt(Calendar.getInstance());
        }
        transaction.setAmounts(adjustTransactionRequest.getAmounts());
        transaction.setStatus(TransactionStatus.CAPTURED);
        transaction.setUpdatedAt(Calendar.getInstance());
        try {
            TRANSACTION_CACHE.put(transaction.getId(), transaction);
            if (listener != null) {
                listener.onResponse(transaction, requestId, null);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateTransaction(String transactionId, AdjustTransactionRequest adjustTransactionRequest, String requestId, IPoyntTransactionServiceListener listener) {
        Log.d(TAG, "UPDATE TRANSACTION: " + transactionId);
        // get cached transaction
        Transaction transaction = TRANSACTION_CACHE.get(UUID.fromString(transactionId));
        if (transaction == null) {
            // if we did not find one just generate a new one
            transaction = new Transaction();
            transaction.setId(UUID.fromString(transactionId));
            transaction.setCreatedAt(Calendar.getInstance());
        }
        transaction.setAmounts(adjustTransactionRequest.getAmounts());
        transaction.setUpdatedAt(Calendar.getInstance());
        try {
            TRANSACTION_CACHE.put(transaction.getId(), transaction);
            listener.onResponse(transaction, requestId, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void voidTransaction(String transactionId,
                                EMVData emvData,
                                String requestId,
                                IPoyntTransactionServiceListener listener) {
        // get cached transaction
        Transaction transaction = TRANSACTION_CACHE.get(UUID.fromString(transactionId));
        if (transaction == null) {
            // if we did not find one just generate a new one
            transaction = new Transaction();
            transaction.setId(UUID.fromString(transactionId));
            transaction.setCreatedAt(Calendar.getInstance());
            TransactionAmounts amounts = new TransactionAmounts();
            amounts.setOrderAmount(100l);
            amounts.setTransactionAmount(100l);
            transaction.setAmounts(amounts);
        }
        transaction.setStatus(TransactionStatus.VOIDED);
        transaction.setUpdatedAt(Calendar.getInstance());

        try {
            TRANSACTION_CACHE.put(transaction.getId(), transaction);
            listener.onResponse(transaction, requestId, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void getTransaction(String transactionId, String requestId, IPoyntTransactionServiceListener listener) {
        Log.d(TAG, "CAPTURED TRANSACTION: " + transactionId);
        // get cached transaction
        Transaction transaction = TRANSACTION_CACHE.get(UUID.fromString(transactionId));
        try {
            listener.onResponse(transaction, requestId, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
