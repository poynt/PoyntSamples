package co.poynt.samples.codesamples;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import co.poynt.os.contentproviders.orders.transactions.TransactionsColumns;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;


@SuppressLint("NewApi")
public class TransactionListActivity extends Activity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "TxnListActivity";
    private final int DISPLAY_PAYMENT_REQUEST = 12345;
    private ListView transactionListview;
    private static final int URL_LOADER = 0;
    private String sortOrder = TransactionsColumns.CREATEDAT + " DESC";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        android.app.ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        transactionListview = (ListView) findViewById(R.id.transaction_list_view);
        transactionListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String txnId = (String) view.getTag(R.id.transaction_id);
                Intent displayPaymentIntent = new Intent(Intents.ACTION_DISPLAY_PAYMENT);
                displayPaymentIntent.putExtra(Intents.INTENT_EXTRAS_TRANSACTION_ID, txnId);
                startActivityForResult(displayPaymentIntent, DISPLAY_PAYMENT_REQUEST);
            }
        });

        getLoaderManager().initLoader(URL_LOADER, null, this);

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_transaction_list, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {

        switch (loaderId) {
            case URL_LOADER:
                return new CursorLoader(
                        this,
                        TransactionsColumns.CONTENT_URI_TRXN_WITH_AMOUNT_AND_CARD_AND_BUSINESS_ID,
                        null,
                        null,
                        null,
                        sortOrder
                );
        }
        return null;
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {
        TransactionFilterCursorWrapper cursorWrapper = new TransactionFilterCursorWrapper(data, true);
        TransactionCursorAdapter adapter = new TransactionCursorAdapter(this, cursorWrapper, false);
        transactionListview.setAdapter(adapter);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);
            Log.d(TAG, "onActivityResult: " + payment);
        }
    }
}
