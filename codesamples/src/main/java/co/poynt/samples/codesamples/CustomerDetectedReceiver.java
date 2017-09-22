package co.poynt.samples.codesamples;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import co.poynt.os.contentproviders.orders.transactions.TransactionsColumns;
import co.poynt.os.contentproviders.orders.transactions.TransactionsCursor;
import co.poynt.os.model.Intents;

public class CustomerDetectedReceiver extends BroadcastReceiver {
    private static final String TAG = CustomerDetectedReceiver.class.getSimpleName();

    private final long NOT_FOUND = -1;
    public CustomerDetectedReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long customerId = intent.getLongExtra(Intents.INTENT_EXTRAS_CUSTOMERID, NOT_FOUND);

        if (customerId != NOT_FOUND) {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(TransactionsColumns.CONTENT_URI_TRXN_WITH_AMOUNT_AND_CARD,
                    TransactionsColumns.FULL_PROJECTION,
                    TransactionsColumns.CUSTOMERUSERID + " = ?",
                    new String[]{""+customerId},
                    TransactionsColumns.CREATEDAT + " DESC");

            TransactionsCursor tc = new TransactionsCursor(cursor);
            String lastTransactionId = null;
            while (tc.moveToNext()){
                lastTransactionId = tc.getTransactionid();
                Log.d(TAG, "CreatedAt:" + tc.getCreatedat());
                Log.d(TAG, "Transaction Id:" + lastTransactionId);
                // only need the latest transaction
                break;
            }

            tc.close();
            cursor.close();

            if (lastTransactionId !=null){
                //call IPoyntTransactionService.getTransaction if you need to get transaction details
            }
        }
    }
}
