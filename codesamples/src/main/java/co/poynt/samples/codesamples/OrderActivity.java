package co.poynt.samples.codesamples;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import co.poynt.api.model.Business;
import co.poynt.api.model.ClientContext;
import co.poynt.api.model.Fee;
import co.poynt.api.model.FulfillmentStatus;
import co.poynt.api.model.Order;
import co.poynt.api.model.OrderAmounts;
import co.poynt.api.model.OrderItem;
import co.poynt.api.model.OrderItemStatus;
import co.poynt.api.model.OrderStatus;
import co.poynt.api.model.OrderStatuses;
import co.poynt.api.model.Transaction;
import co.poynt.api.model.UnitOfMeasure;
import co.poynt.os.contentproviders.orders.clientcontexts.ClientcontextsColumns;
import co.poynt.os.contentproviders.orders.clientcontexts.ClientcontextsCursor;
import co.poynt.os.contentproviders.orders.orders.OrdersColumns;
import co.poynt.os.contentproviders.orders.orders.OrdersCursor;
import co.poynt.os.contentproviders.orders.orderstatuses.OrderstatusesColumns;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntOrderService;
import co.poynt.os.services.v1.IPoyntOrderServiceListener;

public class OrderActivity extends Activity {
    private IPoyntOrderService orderService;
    private static final String TAG = OrderActivity.class.getName();
    private static final int COLLECT_PAYMENT_REQUEST = 100;
    private Button createOrderBtn;
    private Order currentOrder;
    private String currentOrderId;

    Business b;

    private Button pullOpenOrders;
    private Button completeOrderBtn;
    private Button updateOrderBtn;
    private Button getOrderBtn;
    private Button cancelOrderBtn;
    private TextView resultTextView;
    private TextView orderStatusText;
    private TextView currentOrderTextView;
    private Button saveOrderBtn;
    private Button captureOrder;


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

    private IPoyntOrderServiceListener saveOrderServiceListener = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(final Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "orderResponse poyntError: " + poyntError);
            Log.d(TAG, "orderResponse order: " + order.toString());
            if (order != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String s  = "ORDER SAVED\n" + "Order Id :" + order.getId().toString() + "\n";
                        resultTextView.setText(s);
                        currentOrderTextView.setText("");
                        orderStatusText.setText("ORDER SAVED");
                        disableButtons();
                        Toast.makeText(OrderActivity.this, "Saved Order: " + order.getId(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
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
                        Toast.makeText(OrderActivity.this, "Created Order: " + order.getId(), Toast.LENGTH_SHORT).show();
                        showOrderItems(order);
                        resultTextView.append("Employee Id :" + order.getContext().getEmployeeUserId().toString());
                        orderStatusText.setText("ORDER CREATED");
                    }
                });
            }
        }
    };

    private IPoyntOrderServiceListener cancelOrderListener = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(final Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "orderResponse poyntError: " + poyntError);
            Log.d(TAG, "orderResponse order: " + order.toString());
            Log.d(TAG, "CANCELLED_ORDER" + order.toString());
            if (order != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String s  = "ORDER CANCELLED\n" + "Order Id :" + order.getId().toString() + "\n";
                        resultTextView.setText(s);
                        currentOrderTextView.setText("");
                        orderStatusText.setText("ORDER CANCELLED");
                        disableButtons();
                        Toast.makeText(OrderActivity.this, "Cancelled Order: " + order.getId(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    private IPoyntOrderServiceListener getOrderListener = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(final Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "orderResponse poyntError: " + poyntError);
            Log.d(TAG, "orderResponse order: " + order.toString());
            Log.d(TAG, "RECEIVED_ORDER" + order.toString());
            if (order != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String s  = "FETCHED ORDER\n" + "Order Id :" + order.getId().toString() + "\n";
                        resultTextView.setText(s);
                        orderStatusText.setText("ORDER FETCHED");
                        Toast.makeText(OrderActivity.this, "Fetched Order: " + order.getId(), Toast.LENGTH_SHORT).show();
                        showOrderItems(order);
                        enableButtons();
                    }
                });
            }
        }
    };

    private IPoyntOrderServiceListener completeOrderListener = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(final Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "orderResponse poyntError: " + poyntError);
            Log.d(TAG, "orderResponse order: " + order.toString());
            Log.d(TAG, "PROCESSED_ORDER" + order.toString());
            if (order != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String s  = "COMPLETED ORDER" + "Order Id :" + order.getId().toString() + "\n";
                        resultTextView.setText(s);
                        orderStatusText.setText("ORDER COMPLETED");
                        showOrderItems(order);
                        Toast.makeText(OrderActivity.this, "Completed Order: " + order.getId(), Toast.LENGTH_SHORT).show();
                        captureOrder.setEnabled(true);
                    }
                });
            }
        }
    };
    private IPoyntOrderServiceListener updateOrderLister = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(final Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "orderResponse poyntError: " + poyntError);
            Log.d(TAG, "orderResponse order: " + order.toString());
            Log.d(TAG, "UPDATED_ORDER :" + order.toString());
            if (order != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String s  = "UPDATED ORDER" + "Order Id :" + order.getId().toString() + "\n";
                        resultTextView.setText(s);
                        orderStatusText.setText("ORDER UPDATED");
                        showOrderItems(order);
                        Toast.makeText(OrderActivity.this, "Updated Order: " + order.getId(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    private IPoyntOrderServiceListener captureOrderListener = new IPoyntOrderServiceListener.Stub() {
        @Override
        public void orderResponse(final Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "Capture order response received");
            if (poyntError != null){
                Log.d(TAG, "Error received while capturing order");
                Log.d(TAG, poyntError.toString());
            } else {
                Log.d(TAG, "Capture order successful");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (order != null) {
                            String s  = "CAPTURED ORDER" + "Order Id :" + order.getId().toString() + "\n";
                            resultTextView.setText(s);
                            orderStatusText.setText("ORDER CAPTURED");
                            showOrderItems(order);
                            Toast.makeText(OrderActivity.this, "Captured Order: " + order.getId(), Toast.LENGTH_SHORT).show();
                            captureOrder.setEnabled(false);
                        }
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        bindViews();
    }

    private void bindViews() {
        pullOpenOrders = findViewById(R.id.pullOpenOrders);
        completeOrderBtn = findViewById(R.id.completeOrder);
        updateOrderBtn = findViewById(R.id.updateOrder);
        getOrderBtn = findViewById(R.id.getOrder);
        cancelOrderBtn = findViewById(R.id.cancelOrder);
        resultTextView = findViewById(R.id.resultText);
        orderStatusText = findViewById(R.id.orderStatus);
        currentOrderTextView = findViewById(R.id.currentOrderId);
        saveOrderBtn = findViewById(R.id.saveOrder);
        captureOrder = findViewById(R.id.captureOrder);

        pullOpenOrders.setOnClickListener(this::pullOpenOrdersClicked);
        completeOrderBtn.setOnClickListener(this::completeOrdersClicked);
        updateOrderBtn.setOnClickListener(this::updateOrder);
        getOrderBtn.setOnClickListener(this::getOrder);
        cancelOrderBtn.setOnClickListener(this::cancelOrder);
        saveOrderBtn.setOnClickListener(this::saveOrder);
        captureOrder.setOnClickListener(this::captureOrderClicked);

        createOrderBtn = findViewById(R.id.createOrder);
        createOrderBtn.setOnClickListener(v -> {
            currentOrder = generateOrder();
            currentOrderId = currentOrder.getId().toString();
            currentOrderTextView.setText(currentOrderId);
            enableButtons();
            try{
                orderService.createOrder(currentOrder, UUID.randomUUID().toString(), createOrderServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
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
        item1.setId(1000);
        item1.setUnitPrice(100l);
        item1.setQuantity(1.0f);
        item1.setUnitOfMeasure(UnitOfMeasure.EACH);
        item1.setStatus(OrderItemStatus.ORDERED);
        item1.setTax(0l);
        item1.setDiscount(0l);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        // these are the only required fields for second screen display
        item2.setName("Item2");
        item2.setId(1001);
        item2.setUnitPrice(100l);
        item2.setQuantity(1.0f);
        item2.setTax(0l);
        item2.setDiscount(0l);
        item2.setUnitOfMeasure(UnitOfMeasure.EACH);
        item2.setStatus(OrderItemStatus.ORDERED);
        items.add(item2);


        OrderItem item3 = new OrderItem();
        // these are the only required fields for second screen display
        item3.setName("Item3");
        item3.setId(1002);
        item3.setUnitPrice(100l);
        item3.setQuantity(2.0f);
        item3.setStatus(OrderItemStatus.ORDERED);
        item3.setUnitOfMeasure(UnitOfMeasure.EACH);
        item3.setTax(0l);
        item3.setFee(20l);
        item3.setDiscount(0l);
        items.add(item3);
        order.setItems(items);

        BigDecimal subTotal = new BigDecimal(0);
        for (OrderItem item : items) {
            BigDecimal price = new BigDecimal(item.getUnitPrice());
            price.setScale(2, RoundingMode.HALF_UP);
            price = price.multiply(new BigDecimal(item.getQuantity()));
            subTotal = subTotal.add(price);
        }

        Log.d(TAG,"Setting oder Fee");
        Fee orderFee = new Fee();
        orderFee.setAmount(200l);
        orderFee.setId(2222l);
        orderFee.setName("Test Fee");
        orderFee.setAppliedBeforeTax(true);

        ArrayList<Fee> fees = new ArrayList<>();
        fees.add(orderFee);
        order.setFees(fees);

        OrderAmounts amounts = new OrderAmounts();
        amounts.setCurrency("USD");
        amounts.setSubTotal(subTotal.longValue());
        amounts.setNetTotal(subTotal.longValue());
        long discAmount = 0l;
        for (OrderItem item: items){
            discAmount -= item.getDiscount();
        }
        amounts.setDiscountTotal(discAmount);
        amounts.setTaxTotal(0l);
        long feeAmount = 0l;

        for (OrderItem item: items){
            if(item.getFee() != null) {
                feeAmount += item.getFee();
            }
        }
        for (Fee fee: order.getFees()) {
            if(fee.getAmount() != null) {
                feeAmount += fee.getAmount();
            }
        }
        amounts.setFeeTotal(feeAmount);
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
    public void pullOpenOrdersClicked(View view) {

        String lastOrderId = null;

        String[] mProjection = OrderstatusesColumns.FULL_PROJECTION;
        String mSelectionClause = OrderstatusesColumns.FULFILLMENTSTATUS + "= ?";
        String[] mSelectionArgs = {OrderStatus.OPENED.status()};
        String mSortOrder = null;
        Cursor cursor = getContentResolver().query(OrdersColumns.CONTENT_URI_WITH_NETTOTAL_TRXN_STATUS,
                mProjection, mSelectionClause, mSelectionArgs, mSortOrder);
        OrdersCursor orderCursor = new OrdersCursor(cursor);
        if (orderCursor != null) {
            if (orderCursor.getCount() > 0) {
                resultTextView.setText("Found OPEN orders");
                orderStatusText.setText("FOUND OPEN ORDERS");
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
                resultTextView.append(lastOrderId + "\n");
                getOrderContextFromDB(lastOrderId);
            }

        }
        orderCursor.close();
        cursor.close();
//        try {
//            if (lastOrderId != null) {
//                orderService.getOrder(lastOrderId, UUID.randomUUID().toString(), orderServiceListener);
//            }
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "Received onActivityResult (" + requestCode + ")");
        // Check which request we're responding to
        if (requestCode == COLLECT_PAYMENT_REQUEST) {
            Log.d("ORDER_ACTIVITY", "Received onActivityResult from Payment Action");
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);
                    Log.d("ORDER_ACTIVITY", "RESULT_OK");
                    Log.d("ORDER_ACTIVITY", String.valueOf(payment.getTransactions().size()));
                    Transaction transaction = payment.getTransactions().get(0);
                    if(transaction.getReceiptPhone()!=null) {
                        Log.d("RECEIPT_PHONE", transaction.getReceiptPhone().toString());
                    }
                    if( transaction.getReceiptEmailAddress()!=null)
                        Log.d("RECEIPT_EMAIL", transaction.getReceiptEmailAddress());
                    if (payment != null) {
                        //save order
                        if (payment.getOrder() != null) {
                            Log.d(TAG, "CURRENT_ORDER: " + currentOrder);
                            Log.d(TAG, "PAYMENT_ORDER: " + payment.getOrder());
                            Order order = payment.getOrder();
                            order.setTransactions(Collections.singletonList(payment.getTransactions().get(0)));

                            ClientContext clientContext = new ClientContext();
                            order.setContext(clientContext);

                            resultTextView.append("RETURNED ORDER FROM PAYMENT OBJECT\n");
                            showOrderItems(order);
//                            new completeOrderTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, payment.getOrder());
                            try {
                                Log.d(TAG, order.toString());
//                                orderService.updateOrder(order.getId().toString(), order, UUID.randomUUID().toString(), updateOrderLister);
                                orderService.completeOrder(order.getId().toString(), order, UUID.randomUUID().toString(),
                                        completeOrderListener);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }
            }
        }
    }

    public void completeOrdersClicked(View view) {
        String currencyCode = NumberFormat.getCurrencyInstance().getCurrency().getCurrencyCode();

        Payment payment = new Payment();
        String lastReferenceId = UUID.randomUUID().toString();
        payment.setReferenceId(lastReferenceId);

        payment.setCurrency(currencyCode);

        currentOrder = fullFillOrder(currentOrder);
        currentOrder = completeOrderFields(currentOrder);
        showOrderItems(currentOrder);
        if (currentOrder != null) {


            payment.setOrder(currentOrder);
            payment.setOrderId(currentOrder.getId().toString());

            // tip can be preset
            //payment.setTipAmount(500l);
            payment.setAmount(currentOrder.getAmounts().getNetTotal());

        } else {
            // some random amount
            payment.setAmount(1200L);
            // here's how tip can be disabled for tip enabled merchants
            // payment.setDisableTip(true);
        }
        try {
            Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
            collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, payment);
            startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_REQUEST);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Poynt Payment Activity not found - did you install PoyntServices?", ex);
        }
    }

    /**
     * Captures the payment transactions associated with the given order through Poynt Cloud
     * and also updates the order in local Poynt Order content providerName.
     * When partial payment captures are required, an order object with the partial capture
     * information can be passed as an argument. When only orderId is provided,
     * the payments are captured completely.
     * */

    public void captureOrderClicked(View view){
        try {
            orderService.captureOrder(currentOrderId, currentOrder, UUID.randomUUID().toString(), captureOrderListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancelOrder(View view){
        try {
            orderService.cancelOrder(currentOrderId, UUID.randomUUID().toString(), cancelOrderListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void getOrder(View view){
        try {
            orderService.getOrder(currentOrderId, UUID.randomUUID().toString(), getOrderListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void saveOrder(View view){
        try{
            orderService.saveOrder(fullFillOrder(currentOrder), UUID.randomUUID().toString(), saveOrderServiceListener);
            currentOrder = null;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void updateOrder(View view){
        List<OrderItem> items = new ArrayList<>();
        items = currentOrder.getItems();
        OrderItem item4 = new OrderItem();
        item4.setName("Item4");
        item4.setId(1029);
        item4.setUnitPrice(400l);
        item4.setQuantity(1.0f);
        item4.setUnitOfMeasure(UnitOfMeasure.EACH);
        item4.setStatus(OrderItemStatus.ORDERED);
        item4.setTax(0l);
        item4.setDiscount(10l);
        items.add(item4);

        OrderItem item5 = new OrderItem();
        // these are the only required fields for second screen display
        item5.setName("Item5");
        item5.setId(1035);
        item5.setUnitPrice(500l);
        item5.setQuantity(1.0f);
        item5.setTax(0l);
        item5.setDiscount(10l);
        item5.setUnitOfMeasure(UnitOfMeasure.EACH);
        item5.setStatus(OrderItemStatus.ORDERED);
        items.add(item5);

        currentOrder.setItems(items);

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
        amounts.setNetTotal(subTotal.longValue());
        amounts.setDiscountTotal(20l);
        amounts.setFeeTotal(0l);
        amounts.setTaxTotal(0l);



        currentOrder.setAmounts(amounts);

        ArrayList<OrderItem> updatedOrderItems = new ArrayList<>();
        for (OrderItem item: currentOrder.getItems()){
            OrderItem updatedItem = item;
            updatedItem.setStatus(OrderItemStatus.FULFILLED);
            updatedOrderItems.add(updatedItem);
        }
        currentOrder.setItems(updatedOrderItems);

        StringBuilder string = new StringBuilder();
        string.append("Updated order\n");
        showOrderItems(currentOrder);

        Order tempOrder = fullFillOrder(currentOrder);

        try {
            orderService.updateOrder(currentOrderId, tempOrder, UUID.randomUUID().toString(), updateOrderLister);
        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }

    public void disableButtons(){
        completeOrderBtn.setEnabled(false);
        cancelOrderBtn.setEnabled(false);
        saveOrderBtn.setEnabled(false);
        updateOrderBtn.setEnabled(false);

    }

    public void enableButtons(){
        updateOrderBtn.setEnabled(true);
        completeOrderBtn.setEnabled(true);
        getOrderBtn.setEnabled(true);
        cancelOrderBtn.setEnabled(true);
        saveOrderBtn.setEnabled(true);
    }

    public void showOrderItems(final Order ordr){
        if (ordr != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    StringBuilder s = new StringBuilder();
                     s.append("**** Order Items ****\n");
                    for (OrderItem o: ordr.getItems()){
                        s.append(o.toString());
                        s.append("\n****************\n");
                    }
                    s.append("Order Status  " + ordr.getStatuses().getStatus().name() + "\n");
                    s.append("Order Fulfillment Status  " +ordr.getStatuses().getFulfillmentStatus().name()+"\n");
                    s.append("Net Total " + ordr.getAmounts().getNetTotal().toString() + "\n");
                    s.append("-------------------------------------------------------------------\n");
                    resultTextView.append(s.toString());
                    currentOrder = ordr;
                }
            });
        }
    }

    public Order fullFillOrder(Order order){
        ArrayList<OrderItem> itemList = new ArrayList<>();
        for (OrderItem order1: order.getItems()) {
            order1.setStatus(OrderItemStatus.FULFILLED);
            itemList.add(order1);
        }
        order.setItems(itemList);
        return order;
    }

    public Order completeOrderFields(Order order){
        OrderStatuses status = order.getStatuses();
        status.setFulfillmentStatus(FulfillmentStatus.FULFILLED);
        status.setStatus(OrderStatus.COMPLETED);
        order.setStatuses(status);
        return order;
    }

    public void getOrderContextFromDB(String orderId){
        String[] mProjection = ClientcontextsColumns.FULL_PROJECTION;
        String mSelectionClause = ClientcontextsColumns.LINKEDID + "= ?";
        String[] mSelectionArgs = {orderId};
        String mSortOrder = null;
        Cursor cursor = getContentResolver().query(ClientcontextsColumns.CONTENT_URI,
                mProjection, mSelectionClause, mSelectionArgs, mSortOrder);
        ClientcontextsCursor clientcontextsCursor = new ClientcontextsCursor(cursor);
        if (clientcontextsCursor != null) {
            if (clientcontextsCursor.getCount() > 0) {
                clientcontextsCursor.moveToNext();
                resultTextView.append("employee Id: " + clientcontextsCursor.getEmployeeuserid().toString()+"\n");
            }
        }
        clientcontextsCursor.close();
        cursor.close();

    }
}
