package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.poynt.api.model.Business;
import co.poynt.api.model.Order;
import co.poynt.api.model.OrderAmounts;
import co.poynt.api.model.OrderItem;
import co.poynt.api.model.OrderItemStatus;
import co.poynt.api.model.OrderStatus;
import co.poynt.api.model.OrderStatuses;
import co.poynt.api.model.UnitOfMeasure;
import co.poynt.os.contentproviders.orders.orders.OrdersColumns;
import co.poynt.os.contentproviders.orders.orders.OrdersCursor;
import co.poynt.os.contentproviders.orders.orderstatuses.OrderstatusesColumns;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntOrderService;
import co.poynt.os.services.v1.IPoyntOrderServiceListener;

public class OrderActivity extends Activity {
    private IPoyntOrderService orderService;
    private static final String TAG = OrderActivity.class.getName();
    private Button createOrderBtn;

    Business b;

    @Bind(R.id.pullOpenOrders)
    Button pullOpenOrders;
    @Bind(R.id.createOrderText)
    TextView createOrderText;
    @Bind(R.id.openOrdersTitle)
    TextView openOrdersTitle;
    @Bind(R.id.openOrdersText)
    TextView openOrdersText;

    private ServiceConnection orderServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected ");
            orderService = IPoyntOrderService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected ");
        }
    };
    private IPoyntOrderServiceListener orderServiceListener = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "orderResponse poyntError: " + poyntError);
            Log.d(TAG, "orderResponse order: " + order.toString());
        }
    };

    private IPoyntOrderServiceListener createOrderServiceListener = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(final Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "orderResponse poyntError: " + poyntError);
            Log.d(TAG, "orderResponse order: " + order.toString());
            if (order != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        createOrderText.setText("SUCCESS");
                        Toast.makeText(OrderActivity.this, "Created Order: " + order.getId(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    public void onOrderButtonClicked(View view) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        createOrderBtn = (Button) findViewById(R.id.createOrderBtn);
        createOrderBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    orderService.createOrder(generateOrder(), UUID.randomUUID().toString(), createOrderServiceListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        ButterKnife.bind(this);
    }

    protected void onResume() {
        super.onResume();
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_ORDER_SERVICE),
                orderServiceConnection, BIND_AUTO_CREATE);
    }

    protected void onPause() {
        super.onPause();
        unbindService(orderServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_order, menu);
        return true;
    }

    @Override
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

    private Order generateOrder() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        List<OrderItem> items = new ArrayList<>();
        OrderItem item1 = new OrderItem();
        // these are the only required fields for second screen display
        item1.setName("Item1");
        item1.setUnitPrice(100l);
        item1.setQuantity(1.0f);
        item1.setUnitOfMeasure(UnitOfMeasure.EACH);
        item1.setStatus(OrderItemStatus.ORDERED);
        item1.setTax(0l);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        // these are the only required fields for second screen display
        item2.setName("Item2");
        item2.setUnitPrice(100l);
        item2.setQuantity(1.0f);
        item2.setTax(0l);
        item2.setUnitOfMeasure(UnitOfMeasure.EACH);
        item2.setStatus(OrderItemStatus.ORDERED);
        items.add(item2);


        OrderItem item3 = new OrderItem();
        // these are the only required fields for second screen display
        item3.setName("Item3");
        item3.setUnitPrice(100l);
        item3.setQuantity(2.0f);
        item3.setStatus(OrderItemStatus.ORDERED);
        item3.setUnitOfMeasure(UnitOfMeasure.EACH);
        item3.setTax(0l);
        items.add(item3);
        order.setItems(items);

        BigDecimal subTotal = new BigDecimal(0);
        for (OrderItem item : items) {
            BigDecimal price = new BigDecimal(item.getUnitPrice());
            price.setScale(2, RoundingMode.HALF_UP);
            price = price.multiply(new BigDecimal(item.getQuantity()));
            subTotal = subTotal.add(price);
        }

        OrderAmounts amounts = new OrderAmounts();
        amounts.setCurrency("USD");
        amounts.setSubTotal(subTotal.longValue());
        order.setAmounts(amounts);

        OrderStatuses orderStatuses = new OrderStatuses();
        orderStatuses.setStatus(OrderStatus.OPENED);
        order.setStatuses(orderStatuses);
        return order;
    }

    /**
     * @param view this method will use the local content provider to query for open orders
     *             and will pull the last order using OrderService
     */
    @OnClick(R.id.pullOpenOrders)
    public void pullOpenOrdersClicked(View view) {

        String lastOrderId = null;

        String[] mProjection = {OrderstatusesColumns.ORDERID};
        String mSelectionClause = OrderstatusesColumns.FULFILLMENTSTATUS + "= ?";
        String[] mSelectionArgs = {OrderStatus.OPENED.status()};
        String mSortOrder = null;

        Cursor cursor = getContentResolver().query(OrdersColumns.CONTENT_URI_WITH_NETTOTAL_TRXN_STATUS_OPEN,
                mProjection, mSelectionClause, mSelectionArgs, mSortOrder);
        OrdersCursor orderCursor = new OrdersCursor(cursor);
        if (orderCursor != null) {
            if (orderCursor.getCount() > 0){
                openOrdersTitle.setText("Found OPEN orders");
            }
            Log.d(TAG, "pullOpenOrdersClicked: " + orderCursor.getCount());
            while (orderCursor.moveToNext()) {
//                    Log.d(TAG, "order id: " + cursor.getString(0));
//                    Log.d(TAG, "customer id: " + cursor.getString(1));
//                    Log.d(TAG, "created at: " + cursor.getString(2));
                lastOrderId = orderCursor.getOrderid();
                Log.d(TAG, "-------------------------------");
                Log.d(TAG, "order id: " + lastOrderId);
                Log.d(TAG, "customer user id: " + orderCursor.getCustomeruserid());
                Log.d(TAG, "order Number: " + orderCursor.getOrdernumber());
                openOrdersText.append(lastOrderId + "\n");
            }

        }
        orderCursor.close();
        cursor.close();
        try {
            if (lastOrderId != null) {
                orderService.getOrder(lastOrderId, UUID.randomUUID().toString(), orderServiceListener);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
