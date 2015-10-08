package co.poynt.samples.customdiscount;

import android.app.Service;
import android.content.ComponentName;
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
import java.util.ArrayList;
import java.util.List;

import co.poynt.api.model.Discount;
import co.poynt.api.model.Order;
import co.poynt.api.model.OrderItem;
import co.poynt.api.model.ProcessorResponse;
import co.poynt.api.model.ProviderVerification;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntCustomDiscountService;
import co.poynt.os.services.v1.IPoyntCustomDiscountServiceListener;
import timber.log.Timber;

/**
 * This class implements sample custom payment service that show how to
 * support custom payment method like gift card in Poynt OS.
 */
public class CustomCouponService extends Service {

    /**
     * Keys are obtained from Poynt developer portal while signing up for updloading a
     * new APK.
     */
    private PublicKey publicKey;
    private PrivateKey privateKey;

    private List<String> saved_coupons = new ArrayList<>();
    private String saved_customerId = null;

    public CustomCouponService() {
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

    private final IPoyntCustomDiscountService.Stub mBinder = new IPoyntCustomDiscountService.Stub() {

        @Override
        public void applyDiscount(String requestId, String customerId,
                                  Order order,
                                  List<String> couponsList,
                                  IPoyntCustomDiscountServiceListener iPoyntDiscountServiceListener)
                throws RemoteException {

            if (customerId != null) {
                saved_customerId = customerId;
            }
            if (couponsList != null && !couponsList.isEmpty()) {
                saved_coupons = couponsList;
            }

            Timber.d("applyDiscount  (%s)", order.getId().toString());
            new ApplyDiscountTask(requestId, saved_customerId, order, saved_coupons,
                    iPoyntDiscountServiceListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        public void confirmDiscount(String requestId, Order order, IPoyntCustomDiscountServiceListener iPoyntCustomDiscountServiceListener) throws RemoteException {
            Timber.d("confirmDiscount  (%s)", order.getId().toString());
            saved_coupons = null;
            saved_customerId = null;
            new ConfirmDiscountTask(requestId, order,
                    iPoyntCustomDiscountServiceListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        public void cancelDiscount(String requestId, Order order, IPoyntCustomDiscountServiceListener iPoyntCustomDiscountServiceListener) throws RemoteException {
            Timber.d("cancelDiscount  (%s)", order.getId().toString());
            saved_coupons = null;
            saved_customerId = null;
            new CancelDiscountTask(requestId, order,
                    iPoyntCustomDiscountServiceListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

    };

    private static long getOrderItemPrice(OrderItem item) {
        long price = 0;
        if (item != null && item.getUnitPrice() != null && item.getQuantity() != null) {
            price += (long) (item.getUnitPrice() * item.getQuantity());
        }
        return price;
    }

    public static Long getSubtotal(Order order) {
        long total = 0;
        if (order != null && order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                total += getOrderItemPrice(item);
            }
        }
        return total;
    }

    public class ApplyDiscountTask extends AsyncTask<Void, Void, Order> {

        private IPoyntCustomDiscountServiceListener callback;
        private String requestId;
        private String customerId;
        private List<String> couponsList;
        private Order order;
        private PoyntError poyntError;

        public ApplyDiscountTask(String requestId, String customerId,
                                 Order order,
                                 List<String> couponsList,
                                 IPoyntCustomDiscountServiceListener
                                         iPoyntDiscountServiceListener) {
            this.callback = iPoyntDiscountServiceListener;
            this.requestId = requestId;
            this.customerId = customerId;
            this.order = order;
            this.couponsList = couponsList;
            Timber.d("apply discount for requestId (%s)", this.requestId);
        }


        @Override
        protected Order doInBackground(Void... p) {
            // validate the coupon code.
            // check to see if we already applied this coupon to the order.
            if (order.getDiscounts() != null && !order.getDiscounts().isEmpty()) {
                order.setDiscounts(null);
            }

            // Check to see if we have a coupon available.
            if (couponsList != null && !couponsList.isEmpty()) {
                for (String couponCode : couponsList) {
                    // validate the coupon here.
                    //add discount to the order object.
                    Discount discount = new Discount();
                    discount.setCustomName(" Poynt 10% order discount");

                    // some discount id to identify this discount.
                    discount.setId(TimeBasedUUIDGenerator.generateId().toString());

                    // 10% discount.
                    long total = (long) ((getSubtotal(order) * 10) / 100f);

                    // Order level discount amounts are negative
                    discount.setAmount(total * -1);
                    List<Discount> discounts = order.getDiscounts();
                    if (discounts == null) {
                        discounts = new ArrayList<>();
                    }
                    addProcessorResponse(discount);
                    discounts.add(discount);
                    order.setDiscounts(discounts);

                    // Apply item level discount
                    // For this sample we will apply discount to first item.
                    if (order.getItems() != null && !order.getItems().isEmpty()) {
                        OrderItem orderItem = order.getItems().get(0);
                        // clear previously applied discount if any.
                        orderItem.setDiscounts(null);
                        Discount itemDiscount = new Discount();

                        itemDiscount.setCustomName(" Poynt 5% item discount");

                        // some discount id to identify this discount.
                        itemDiscount.setId(TimeBasedUUIDGenerator.generateId().toString());
                        // 5% discount.
                        long itemTotal = (long) ((getOrderItemPrice(orderItem) * 5) / 100f);

                        // Order item level discount amounts are always positive
                        itemDiscount.setAmount(itemTotal);
                        List<Discount> itemDiscounts = orderItem.getDiscounts();
                        if (itemDiscounts == null) {
                            itemDiscounts = new ArrayList<>();
                        }
                        addProcessorResponse(itemDiscount);
                        itemDiscounts.add(itemDiscount);
                        orderItem.setDiscounts(itemDiscounts);
                    }
                }
            }
            return order;
        }

        @Override
        protected void onPostExecute(Order order) {
            super.onPostExecute(order);
            try {
                // if customerId is not null and we havn't collected qr code
                // then we will launch activity to get qr code
                // from the merchant side.
                if (customerId != null && couponsList == null
                        || (couponsList != null && couponsList.isEmpty())) {
                    // prepare intent to launch activity.
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("co.poynt.customtender",
                            "co.poynt.customtender.CouponEntryActivity"));
                    // start the activity.
                    // order will be passed back to the intent as a extra bundle.
                    callback.onLaunchActivity(requestId, intent, order);
                    return;
                }

                if (order != null) {
                    Timber.d("apply discount for order (%s)", (order != null ? order.getId() : "-failed-"));
                    callback.onSuccess(requestId, "processed", order);
                }

                if (poyntError != null) {
                    PoyntError poyntError = new PoyntError(PoyntError.CODE_UNAUTHORIZED);
                    poyntError.setReason(" Failed to get order");
                    Timber.e("Error received (%s)", poyntError.toString());
                    callback.onError(requestId, poyntError);
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public class CancelDiscountTask extends AsyncTask<Void, Void, Order> {

        private IPoyntCustomDiscountServiceListener callback;
        private String requestId;
        private Order order;

        public CancelDiscountTask(String requestId,
                                  Order order,
                                  IPoyntCustomDiscountServiceListener
                                          iPoyntDiscountServiceListener) {
            this.callback = iPoyntDiscountServiceListener;
            this.requestId = requestId;
            this.order = order;
            Timber.d("cancel discount order for requestId (%s)", this.requestId);
        }

        @Override
        protected Order doInBackground(Void... p) {
            // Cancel/void the discount and coupons
            // and return appropriate callback.
            return order;
        }

        @Override
        protected void onPostExecute(Order order) {
            super.onPostExecute(order);
            try {
                if (order != null) {
                    Timber.d("cancel discount for order (%s)", (order != null ? order.getId() : "-failed-"));
                    callback.onSuccess(requestId, "cancelled", order);
                } else {
                    Timber.e(" Discount cancel error");
                    callback.onError(requestId, new PoyntError(PoyntError.CODE_UNAUTHORIZED));
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public class ConfirmDiscountTask extends AsyncTask<Void, Void, Order> {

        private IPoyntCustomDiscountServiceListener callback;
        private String requestId;
        private Order order;

        public ConfirmDiscountTask(String requestId,
                                   Order order,
                                   IPoyntCustomDiscountServiceListener
                                           iPoyntDiscountServiceListener) {
            this.callback = iPoyntDiscountServiceListener;
            this.requestId = requestId;
            this.order = order;
            Timber.d("confirm discount for requestId (%s)", this.requestId);
        }

        @Override
        protected Order doInBackground(Void... p) {
            return order;
        }

        @Override
        protected void onPostExecute(Order order) {
            super.onPostExecute(order);
            try {
                if (order != null) {
                    Timber.d("confirm discount for order (%s)", (order != null ? order.getId() : "-failed-"));
                    callback.onSuccess(requestId, "completed", order);
                } else {
                    Timber.e(" confirm discount error");
                    callback.onError(requestId, new PoyntError(PoyntError.CODE_UNAUTHORIZED));
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void addProcessorResponse(Discount discount) {
        // set the signature to the discount.
        String message = discount.getId() + String.valueOf(discount.getAmount());
        String signature = KeyUtils.signMessage(message, privateKey);

        ProcessorResponse processorResponse = new ProcessorResponse();
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
        discount.setProcessorResponse(processorResponse);
        // same provider as defined in coupons_capabilities.xml.
        discount.setProvider("Poynt Coupon");
    }

}