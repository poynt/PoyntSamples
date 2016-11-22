package co.poynt.samples.codesamples;


import android.database.Cursor;
import android.database.CursorWrapper;
import android.util.Log;

import co.poynt.api.model.TransactionAction;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.contentproviders.orders.transactions.TransactionsColumns;

public class TransactionFilterCursorWrapper extends CursorWrapper {
    private int[] index;
    private int count = 0;
    private int pos = 0;
    private boolean filter;

    public TransactionFilterCursorWrapper(Cursor cursor, boolean filter) {
        super(cursor);
        this.filter = filter;
        this.count = super.getCount();
        this.index = new int[this.count];
        for (int i = 0; i < this.count; i++) {
            super.moveToPosition(i);
            TransactionAction txnAction = null;
            TransactionStatus txnStatus = null;
            String txnActionStr = this.getString(this.getColumnIndex(TransactionsColumns.ACTION));
            if (isNotEmpty(txnActionStr)) {
                txnAction = TransactionAction.findByAction(txnActionStr);
            }
            String txnStatusStr = this.getString(this.getColumnIndex(TransactionsColumns.STATUS));
            if (isNotEmpty(txnStatusStr)) {
                txnStatus = TransactionStatus.findByStatus(txnStatusStr);
            }
            if (!isHidden(txnAction, txnStatus))
                this.index[this.pos++] = i;
        }
        this.count = this.pos;
        this.pos = 0;
        super.moveToFirst();

    }
    private boolean isNotEmpty(String s){
        return ( s!=null && !"".equals(s) );
    }

    private boolean isHidden(TransactionAction txnAction, TransactionStatus txnStatus) {
        boolean isHidden = false;
        if (!filter) {
            Log.d("TxnCursorWrapper", "No filter requested");
            return false;
        }

        if (txnStatus != null) {
            if (txnStatus.equals(TransactionStatus.CREATED)) {
                // Hide txns with created status as its an Poynt internal only status.
                isHidden = true;
            } else if ((txnAction == TransactionAction.AUTHORIZE &&
                    txnStatus == TransactionStatus.REFUNDED) ||
                    (txnAction == TransactionAction.SALE &&
                            txnStatus == TransactionStatus.REFUNDED)
                    ) {
                // hide refunded auth - since the refunded txn will show up anyways
                isHidden = true;
            } else if (txnAction == TransactionAction.AUTHORIZE &&
                    txnStatus == TransactionStatus.CAPTURED) {
                // authorization txn but already captured - so let's hide this so only capture txn will be displayed
                isHidden = true;
            } else if (txnAction == TransactionAction.CAPTURE && txnStatus == TransactionStatus.REFUNDED) {
                // we will let refund txn to show up
                isHidden = true;
            } else {
                isHidden = false;
            }
        } else {
            isHidden = false;
        }
        return isHidden;
    }

    public boolean move(int offset) {
        return this.moveToPosition(this.pos + offset);
    }

    public boolean moveToNext() {
        return this.moveToPosition(this.pos + 1);
    }

    public boolean moveToPrevious() {
        return this.moveToPosition(this.pos - 1);
    }

    public boolean moveToFirst() {
        return this.moveToPosition(0);
    }

    public boolean moveToLast() {
        return this.moveToPosition(this.count - 1);
    }

    public boolean moveToPosition(int position) {
        if (position >= this.count || position < 0)
            return false;
        return super.moveToPosition(this.index[position]);
    }

    public int getCount() {
        return this.count;
    }

    public int getPosition() {
        return this.pos;
    }
}
