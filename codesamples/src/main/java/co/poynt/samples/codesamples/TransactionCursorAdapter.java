package co.poynt.samples.codesamples;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import co.poynt.api.model.FundingSourceType;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.contentproviders.orders.transactionamounts.TransactionamountsColumns;
import co.poynt.os.contentproviders.orders.transactions.TransactionsCursor;

/**
 * Created by dennis on 1/29/16.
 */
public class TransactionCursorAdapter extends CursorAdapter {
    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd hh:mm");
    Calendar calendar = Calendar.getInstance();

    public TransactionCursorAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    public TransactionCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.transaction_row, parent, false);
    }

    public void bindView(View view, Context context, Cursor data) {
        TransactionsCursor tc = new TransactionsCursor(data);
        TextView txnId = (TextView) view.findViewById(R.id.transaction_id);
        TextView createdAt = (TextView) view.findViewById(R.id.created_at);
        TextView fundingSource = (TextView) view.findViewById(R.id.fundingsource_type);
        TextView amount = (TextView) view.findViewById(R.id.transaction_amount);
        TextView transactionStatus = (TextView) view.findViewById(R.id.status);

        int transactionAmountIndex = data.getColumnIndex(TransactionamountsColumns.TRANSACTIONAMOUNT);

        String txnIdString = tc.getTransactionid();
        view.setTag(R.id.transaction_id, txnIdString);
        // to get the first group of numbers of transaction UUID
        txnIdString = "#" + txnIdString.substring(0, txnIdString.indexOf("-"));

        txnId.setText(txnIdString);


        calendar.setTime(tc.getCreatedat());
        createdAt.setText(dateFormat.format(tc.getCreatedat()));

        fundingSource.setText(getFundingSourceString(tc));

        // TransactionCursor does not have amount, so using Cursor
        long doublePayment = data.getLong(transactionAmountIndex);
        ;
        BigDecimal paymentAmount = new BigDecimal(doublePayment).movePointLeft(2);

        amount.setText("$ " + paymentAmount.toPlainString());
        transactionStatus.setText(
                getTransactionStatus(TransactionStatus.findByStatus(tc.getStatus()))
        );

    }

    private String getFundingSourceString(TransactionsCursor tc) {
        FundingSourceType type = FundingSourceType.findByType(tc.getFundingsourcetype());
        if (type.equals(FundingSourceType.CASH)) {
            return "CASH";
        } else if (type.equals(FundingSourceType.CREDIT_DEBIT)) {
            return "CARD";
        } else if (type.equals(FundingSourceType.CUSTOM_FUNDING_SOURCE)) {
            return "CUSTOM";
        } else {
            return "OTHER";
        }
    }

    private String getTransactionStatus(TransactionStatus status) {
        Log.d("CursorAdapter", "getTransactionStatus: " + status);
        String txnStatus = "unknown";
        switch (status) {
            case AUTHORIZED:
                txnStatus = "AUTHORIZED";
                break;
            case CAPTURED:
                txnStatus = "CAPTURED";
                break;
            case PARTIALLY_REFUNDED:
            case REFUNDED:
                txnStatus = "REFUNDED";
                break;
            case DECLINED:
                txnStatus = "DECLINED";
                break;
            case VOIDED:
                txnStatus = "VOIDED";
                break;
            default:
                // do nothing
                break;
        }
        return txnStatus;
    }
}
