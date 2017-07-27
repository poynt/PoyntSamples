package co.poynt.samples.codesamples;


import android.database.Cursor;
import android.database.CursorWrapper;
import android.util.Log;

import co.poynt.api.model.TransactionAction;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.contentproviders.orders.clientcontexts.ClientcontextsColumns;
import co.poynt.os.contentproviders.orders.transactions.TransactionsColumns;

public class TransactionFilterCursorWrapper extends CursorWrapper {

    private static final String TAG = TransactionFilterCursorWrapper.class.getSimpleName();


    private int[] index;
    private int count = 0;
    private int pos = 0;
    private boolean filter;
    private boolean demoMode;

    public TransactionFilterCursorWrapper(Cursor cursor, boolean filter, String role, String currentUserId) {
        this(cursor, filter, role, currentUserId, false);
    }

    public TransactionFilterCursorWrapper(Cursor cursor, boolean filter, String role, String currentUserId, boolean demoMode) {
        super(cursor);
        this.demoMode = demoMode;
        this.filter = filter;
        if (cursor != null) {
            this.count = super.getCount();
        }
        this.index = new int[this.count];
        for (int i = 0; i < this.count; i++) {
            super.moveToPosition(i);
            TransactionAction txnAction = null;
            TransactionStatus txnStatus = null;
            String txnActionStr = this.getString(this.getColumnIndex(TransactionsColumns.ACTION));
            if (notEmpty(txnActionStr)) {
                txnAction = TransactionAction.findByAction(txnActionStr);
            }
            String txnStatusStr = this.getString(this.getColumnIndex(TransactionsColumns.STATUS));
            if (notEmpty(txnStatusStr)) {
                txnStatus = TransactionStatus.findByStatus(txnStatusStr);
            }

            String employeeUserId = this.getString(this.getColumnIndex(ClientcontextsColumns.EMPLOYEEUSERID));
            String parentID = this.getString(this.getColumnIndex(TransactionsColumns.PARENTID));
            boolean isIdMatches = currentUserId != null && employeeUserId != null && currentUserId.equals(employeeUserId);
            if (!isHidden(txnAction, txnStatus, role, isIdMatches, isVoidedTrx(), isNonRefRefund(parentID)))
                this.index[this.pos++] = i;
        }
        this.count = this.pos;
        this.pos = 0;
        super.moveToFirst();
    }

    public TransactionFilterCursorWrapper(Cursor cursor, boolean filter) {
        super(cursor);
        this.filter = filter;
        if (cursor != null) {
            this.count = super.getCount();
        }
        this.index = new int[this.count];
        for (int i = 0; i < this.count; i++) {
            super.moveToPosition(i);
            TransactionAction txnAction = null;
            TransactionStatus txnStatus = null;
            String txnActionStr = this.getString(this.getColumnIndex(TransactionsColumns.ACTION));
            if (notEmpty(txnActionStr)) {
                txnAction = TransactionAction.findByAction(txnActionStr);
            }
            String txnStatusStr = this.getString(this.getColumnIndex(TransactionsColumns.STATUS));
            if (notEmpty(txnStatusStr)) {
                txnStatus = TransactionStatus.findByStatus(txnStatusStr);
            }

            String parentID = this.getString(this.getColumnIndex(TransactionsColumns.PARENTID));
            if (!isHidden(txnAction, txnStatus, null, false, isVoidedTrx(), isNonRefRefund(parentID)))
                this.index[this.pos++] = i;
        }
        this.count = this.pos;
        this.pos = 0;
        super.moveToFirst();

    }


    private boolean isVoidedTrx() {

        boolean isVoided;
        int voidedInt = this.getInt(this.getColumnIndex(TransactionsColumns.VOIDED));
        if (voidedInt == 1) {
            isVoided = true;
        } else {
            isVoided = false;
        }
        return isVoided;
    }


    private boolean isNonRefRefund(String parentId) {
        // no parent id with refund action means its NON REF
        return parentId == null;
    }

    private boolean isHidden(TransactionAction txnAction,
                             TransactionStatus txnStatus,
                             String role, boolean idMatches,
                             boolean isVoided,
                             boolean isNonRef) {
        boolean isHidden = false;
        if (!filter) {
            Log.d(TAG, "No filter requested");
            return false;
        }

        if (txnStatus != null) {
            if (txnStatus.equals(TransactionStatus.CREATED)) {
                // Hide txns with created status as its an internal only status.
                // https://jira.poynt.co/browse/LMI-63
                isHidden = true;
            } else if ((txnAction == TransactionAction.AUTHORIZE
                    && (txnStatus == TransactionStatus.REFUNDED || txnStatus == TransactionStatus.PARTIALLY_REFUNDED))
                    || (txnAction == TransactionAction.SALE && txnStatus == TransactionStatus.REFUNDED)) {
                // hide refunded auth - since the refunded txn will show up anyways
                isHidden = true;
            } else if (txnAction == TransactionAction.AUTHORIZE &&
                    txnStatus == TransactionStatus.CAPTURED) {
                // authorization txn but already captured - so let's hide this so only capture txn will be displayed
                isHidden = true;
            } else if (txnAction == TransactionAction.CAPTURE
                    && txnStatus == TransactionStatus.DECLINED && isVoided) {
                isHidden = true;
            } else if (txnAction == TransactionAction.REFUND
                    && txnStatus == TransactionStatus.DECLINED && isVoided) {
                if (isNonRef) {
                    isHidden = false;
                } else {
                    isHidden = true;
                }
            } else if (txnAction == TransactionAction.CAPTURE
                    && txnStatus == TransactionStatus.REFUNDED) {
                if(demoMode) {
                    isHidden = false;
                } else {
                    isHidden = true;
                }
            } else if (txnAction == TransactionAction.CAPTURE && txnStatus == TransactionStatus.REFUNDED) {

                // we will let refund txn to show up
                isHidden = true;
            } else if (txnAction == TransactionAction.REFUND && txnStatus == TransactionStatus.VOIDED) {
                // we will let auth txn to show up
                isHidden = true;
            } else {
                isHidden = false;
            }
        } else {
            isHidden = false;
        }

        if (role != null) {
            if (!isHidden) {
                if (role != null && role.equals("E") && !idMatches) {
                    isHidden = true;
                }
            }
        }

        //Ln.d("Transaction action (%s) status (%s) result (%b)", txnAction, txnStatus, isHidden);
        return isHidden;
    }

    @Override
    public boolean move(int offset) {
        return this.moveToPosition(this.pos + offset);
    }

    @Override
    public boolean moveToNext() {
        return this.moveToPosition(this.pos + 1);
    }

    @Override
    public boolean moveToPrevious() {
        return this.moveToPosition(this.pos - 1);
    }

    @Override
    public boolean moveToFirst() {
        return this.moveToPosition(0);
    }

    @Override
    public boolean moveToLast() {
        return this.moveToPosition(this.count - 1);
    }

    @Override
    public boolean moveToPosition(int position) {
        if (position >= this.count || position < 0)
            return false;
        return super.moveToPosition(this.index[position]);
    }

    @Override
    public int getCount() {
        return this.count;
    }

    @Override
    public int getPosition() {
        return this.pos;
    }

    private boolean notEmpty(final String s)
    {
        return !isEmpty(s);
    }

    private boolean isEmpty(final String s)
    {
        return s == null || s.length() == 0;
    }
}
