package co.poynt.samples.codesamples;

import static co.poynt.samples.codesamples.utils.Util.generateTransaction;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import co.poynt.api.model.Discount;
import co.poynt.api.model.ExchangeRate;
import co.poynt.api.model.OrderItem;
import co.poynt.api.model.Transaction;
import co.poynt.os.model.Intents;
import co.poynt.os.model.SecondScreenLabels;
import co.poynt.os.services.v1.IPoyntSecondScreenCheckInListener;
import co.poynt.os.services.v1.IPoyntSecondScreenCodeScanListener;
import co.poynt.os.services.v1.IPoyntSecondScreenDynamicCurrConversionListener;
import co.poynt.os.services.v1.IPoyntSecondScreenEmailEntryListener;
import co.poynt.os.services.v1.IPoyntSecondScreenPhoneEntryListener;
import co.poynt.os.services.v1.IPoyntSecondScreenRatingEntryListener;
import co.poynt.os.services.v1.IPoyntSecondScreenService;
import co.poynt.os.services.v1.IPoyntSecondScreenSurchargeListener;
import co.poynt.os.services.v1.IPoyntSecondScreenTextEntryListener;

public class SecondScreenServiceActivity extends Activity {

    private static final String TAG = SecondScreenServiceActivity.class.getSimpleName();


    Button phoneNumberBtn;
    Button scanQRBtn;
    Button displayItemsBtn;
    Button checkInScreenBtn;
    Button emailBtn;
    Button textEntryBtn;
    Button dccScreenBtn;
    Button collectRatingBtn;
    Button collectSurchargeBtn;
    TextView phoneStatus;
    TextView emailStatus;
    TextView textStatus;
    TextView scanStatus;
    TextView checkinStatus;
    TextView dccStatus;
    TextView ratingStatus;
    TextView surchargeStatus;

    private IPoyntSecondScreenService secondScreenService;
    private ServiceConnection secondScreenServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            secondScreenService = IPoyntSecondScreenService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            secondScreenService = null;
        }
    };
    private IPoyntSecondScreenPhoneEntryListener phoneEntryListener =
            new IPoyntSecondScreenPhoneEntryListener.Stub() {
                @Override
                public void onPhoneEntered(String phone) throws RemoteException {
                    showToast("Captured Phone: " + phone);
                    setStatus(phoneStatus, phone);
                    showWelcomeScreen();
                }

                @Override
                public void onPhoneEntryCanceled() throws RemoteException {
                    showToast("User canceled phone entry");
                }
            };
    private IPoyntSecondScreenCodeScanListener codeScanListener =
            new IPoyntSecondScreenCodeScanListener.Stub() {
                @Override
                public void onCodeScanned(String s) throws RemoteException {
                    showToast("Code scanned: " + s);
                }

                @Override
                public void onCodeEntryCanceled() throws RemoteException {
                    showWelcomeScreen();
                    setStatus(scanStatus, "CANCELED");
                }
            };

    public void phoneNumberButtonClicked(View view) {
        try {
            // @deprecated
            //secondScreenService.collectPhoneNumber(phoneEntryListener);

            secondScreenService.capturePhoneNumber(SecondScreenLabels.DEFAULT, SecondScreenLabels.OK,
                    phoneEntryListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    IPoyntSecondScreenEmailEntryListener emailEntryListener =
            new IPoyntSecondScreenEmailEntryListener.Stub() {
                @Override
                public void onEmailEntered(String s) throws RemoteException {
                    showToast("Captured email: " + s);
                    setStatus(emailStatus, s);
                    showWelcomeScreen();
                }

                @Override
                public void onEmailEntryCanceled() throws RemoteException {
                    showWelcomeScreen();
                }
            };

    public void emailBtnclicked(View view) {
        try {
            //@deprecated
            //secondScreenService.collectEmailAddress(null, emailEntryListener);
            String defaultEmail = "jane@domain.com";
            secondScreenService.captureEmailAddress(defaultEmail, SecondScreenLabels.CANCEL,
                    SecondScreenLabels.CONFIRM, emailEntryListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    IPoyntSecondScreenTextEntryListener textEntryListener =
            new IPoyntSecondScreenTextEntryListener.Stub() {
                @Override
                public void onTextEntered(String s) throws RemoteException {
                    showToast("Captured Text: " + s);
                    setStatus(textStatus, s);
                }

                @Override
                public void onTextEntryCanceled() throws RemoteException {
                    showWelcomeScreen();
                }
            };

    public void textEntryBtnClicked(View view) {
        try {
            secondScreenService.collectTextEntry("Enter Code:", textEntryListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void scanQRCode(View view) {
        try {
            // @deprecated
            // secondScreenService.scanCode(codeScanListener);

            secondScreenService.captureCode(SecondScreenLabels.CANCEL, codeScanListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second_screen_service);

        android.app.ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        bindViews();
    }

    private void bindViews() {
        phoneNumberBtn = findViewById(R.id.phoneNumberBtn);
        scanQRBtn = findViewById(R.id.scanQRBtn);
        displayItemsBtn = findViewById(R.id.displayItemsBtn);
        checkInScreenBtn = findViewById(R.id.checkInScreenBtn);
        emailBtn = findViewById(R.id.emailBtn);
        textEntryBtn = findViewById(R.id.textEntryBtn);
        dccScreenBtn = findViewById(R.id.dccScreenBtn);
        collectRatingBtn = findViewById(R.id.collectRatingBtn);
        collectSurchargeBtn = findViewById(R.id.collectSurchargeBtn);
        phoneStatus = findViewById(R.id.phoneStatus);
        emailStatus = findViewById(R.id.emailStatus);
        textStatus = findViewById(R.id.textStatus);
        scanStatus = findViewById(R.id.scanStatus);
        checkinStatus = findViewById(R.id.checkinStatus);
        dccStatus = findViewById(R.id.dccStatus);
        ratingStatus = findViewById(R.id.collectRatingStatus);
        surchargeStatus = findViewById(R.id.collectSurchargeStatus);

        phoneNumberBtn.setOnClickListener(this::phoneNumberButtonClicked);
        scanQRBtn.setOnClickListener(this::scanQRCode);
        displayItemsBtn.setOnClickListener(v -> showItems());
        checkInScreenBtn.setOnClickListener(v -> showCheckinScreen());
        emailBtn.setOnClickListener(this::emailBtnclicked);
        textEntryBtn.setOnClickListener(this::textEntryBtnClicked);
        dccScreenBtn.setOnClickListener(v -> showDccScreen());
        collectRatingBtn.setOnClickListener(v -> showRatingScreen());
        collectSurchargeBtn.setOnClickListener(v -> showSurchargeScreen());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_SECOND_SCREEN_SERVICE),
                secondScreenServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            secondScreenService.displayWelcome(null, null, null);
        } catch (RemoteException | NullPointerException e) {
            e.printStackTrace();
        }
        unbindService(secondScreenServiceConnection);
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SecondScreenServiceActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setStatus(final TextView textView, final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(msg);
            }
        });
    }

    private void showWelcomeScreen() {
        try {
            secondScreenService.displayWelcome(null, null, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_second_screen, menu);
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

    public void showItems() {
        // create some dummy items to display in second screen
        List<OrderItem> items = new ArrayList<OrderItem>();
        OrderItem item1 = new OrderItem();
        // these are the only required fields for second screen display
        item1.setName("Item1");
        item1.setUnitPrice(100l);
        item1.setQuantity(1.0f);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        // these are the only required fields for second screen display
        item2.setName("Item2");
        item2.setUnitPrice(100l);
        item2.setQuantity(1.0f);

        //item2.setDiscount(-50l);
        List<Discount> discounts = new ArrayList<>();
        Discount discount = new Discount();
        discount.setAmount(-50l);
        discount.setId(UUID.randomUUID().toString());
        discount.setCustomName("My custom discount");

        discounts.add(discount);
        item2.setDiscounts(discounts);

        items.add(item2);

        OrderItem item3 = new OrderItem();
        // these are the only required fields for second screen display
        item3.setName("Item3");
        item3.setUnitPrice(100l);
        item3.setQuantity(2.0f);


        items.add(item3);

        try {
            if (secondScreenService != null) {
                BigDecimal total = new BigDecimal(0);
                for (OrderItem item : items) {
                    BigDecimal price = new BigDecimal(item.getUnitPrice());
                    price.setScale(2, RoundingMode.HALF_UP);
                    price = price.multiply(new BigDecimal(item.getQuantity()));
                    total = total.add(price);
                }
                secondScreenService.showItem(items, total.longValue(), "USD");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    IPoyntSecondScreenCheckInListener checkinScreenListener =
            new IPoyntSecondScreenCheckInListener.Stub() {
                @Override
                public void onCheckIn() throws RemoteException {
                    showToast("Checkin Clicked");
                    setStatus(checkinStatus, "CHECK-IN TAPPED");
                    phoneNumberButtonClicked(null);
                }

                @Override
                public void onSecondScreenBusy() throws RemoteException {
                    //do nothing
                }

            };

    public void showCheckinScreen() {
        try {
//            Bitmap checkin = BitmapFactory.decodeResource(getResources(),R.drawable.button_checkin);
            secondScreenService.displayWelcome("Check-in", null, checkinScreenListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void showDccScreen() {
        ExchangeRate ex = new ExchangeRate();
        ex.setProvider("Citibank UAE"); // printed on the receipt
        ex.setTxnAmount(10000L);
        ex.setTxnCurrency("USD");

        ex.setRate(367326L);
        ex.setRatePrecision(5L); // basically the rate above is 3.67326

        ex.setCardCurrency("AED");
        ex.setMarkupPercentage("250"); // shows the markup in the UI
        ex.setCardAmount(37651L);
        try {
            secondScreenService.captureDccChoice(ex, null, new IPoyntSecondScreenDynamicCurrConversionListener.Stub() {
                @Override
                public void onCurrencyConversionSelected(boolean b) throws RemoteException {
                    Log.d(TAG, "onCurrencyConversionSelected: " + b);
                    setStatus(dccStatus, "DCC option selected");
                    showWelcomeScreen();
                }

                @Override
                public void onCancel() throws RemoteException {
                    Log.d(TAG, "onCancel()");
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void showRatingScreen(){
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo);
        try {
            // Scale image is deprecated, just pass null
            secondScreenService.collectRating(1, 5, 1, "How did we do", bitmap, null, ratingEntryListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void showSurchargeScreen(){
        Transaction txn = generateTransaction(100L);
        Bundle bundle = new Bundle();
        bundle.putParcelable("transaction", txn);
        bundle.putLong("surchargeAmount", 123L);
//        bundle.putString("disclaimer", "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.");
        try {
            // Scale image is deprecated, just pass null
            secondScreenService.showSurchargeScreen(bundle, new IPoyntSecondScreenSurchargeListener.Stub() {
                @Override
                public void onSurchargeAgreed() throws RemoteException {
                    Log.d(TAG, "showSurchargeScreen agreed");
                    setStatus(surchargeStatus, "Surcharge agreed");
                    showWelcomeScreen();
                }

                @Override
                public void onCancel() throws RemoteException {
                    Log.d(TAG, "showSurchargeScreen canceled");
                    setStatus(surchargeStatus, "Surcharge canceled");
                    showWelcomeScreen();
                }
            });
        } catch (RemoteException e) {
        }
    }

    IPoyntSecondScreenRatingEntryListener ratingEntryListener = new IPoyntSecondScreenRatingEntryListener.Stub() {
        @Override
        public void onRatingEntered(int i) throws RemoteException {
            Log.d(TAG, "Entered rating received");
            setStatus(ratingStatus, "Received rating");
            showWelcomeScreen();
        }

        @Override
        public void onRatingEntryCanceled() throws RemoteException {
            Log.d(TAG, "Rating cancelled by user");
            setStatus(ratingStatus, "Cancelled by user");
            showWelcomeScreen();
        }
    };
}
