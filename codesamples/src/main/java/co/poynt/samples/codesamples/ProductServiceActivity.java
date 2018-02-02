package co.poynt.samples.codesamples;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import co.poynt.api.model.CatalogItemWithProduct;
import co.poynt.api.model.CatalogWithProduct;
import co.poynt.api.model.CategoryWithProduct;
import co.poynt.api.model.Product;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntProductCatalogWithProductListener;
import co.poynt.os.services.v1.IPoyntProductService;
import co.poynt.samples.codesamples.utils.Util;

public class ProductServiceActivity extends Activity {

    private static final String TAG = ProductServiceActivity.class.getSimpleName();
    private IPoyntProductService mProductService;
    private Button mGetRegisterCatalogBtn;
    private Button mCreateProductBtn;
    private TextView mConsoleText;
    private ProgressBar mProgress;
    private LinearLayout mProgressBarLayout;
    private int mProgressStatus = 0;
    private boolean mIsCatalogLoaded;

    private TextView getCatalogStatus;

    private ServiceConnection productServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mProductService = IPoyntProductService.Stub.asInterface(service);
            Log.d(TAG, "productService connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mProductService = null;
            Log.d(TAG, "productService disconnected");
        }
    };

    private IPoyntProductCatalogWithProductListener mProdCatWithProdListener = new IPoyntProductCatalogWithProductListener.Stub() {
        @Override
        public void onResponse(CatalogWithProduct catalogWithProduct, PoyntError poyntError) throws RemoteException {
            StringBuilder output = new StringBuilder();

            if (catalogWithProduct.getCategories()!=null && catalogWithProduct.getCategories().size() > 0){
                setStatus(getCatalogStatus, "CATALOG HAS PRODUCTS");
            }
            // products not nested inside a category
            for (CatalogItemWithProduct catalogItem : catalogWithProduct.getProducts()){
                Product product = catalogItem.getProduct();
                output.append("---- ITEM -----\n");
                output.append("Product Name: " + product.getName() + "\n");
                output.append("Product Id: " + product.getId() + "\n");
                output.append("Product Price: " + product.getPrice().getAmount() + "\n");
                Log.d(TAG, "\n"+output);
            }
            for (CategoryWithProduct category : catalogWithProduct.getCategories()){
                output.append("---- Category -----\n");
                output.append("Category Name: " + category.getName() + "\n");
                output.append("Category Id: " + category.getId() + "\n");
                for (CatalogItemWithProduct catalogItem : category.getProducts()){
                    Product product = catalogItem.getProduct();
                    output.append("\t---- ITEM -----\n");
                    output.append("\tProduct Name: " + product.getName() + "\n");
                    output.append("\tProduct Id: " + product.getId() + "\n");
                    output.append("\tProduct Price: " + product.getPrice().getAmount() + "\n");
                }
            }
            final String outputString = output.toString();
            Handler handler = new Handler(getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mConsoleText.setText(outputString);
                    // remove progress bar
                    mProgressBarLayout.removeAllViews();
                    // re-enable button
                    mGetRegisterCatalogBtn.setEnabled(true);
                }
            });
            Log.d(TAG, "\n" + output);
            mIsCatalogLoaded = true;
        }
    };

    private int getProgressStatus(int currentProgress){
        if (mIsCatalogLoaded){
            return 101;
        }
        int step = 1;
        int newProgress = currentProgress + step;
        if (newProgress > 100){
            return currentProgress;
        }else{
            return newProgress;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_service);

        android.app.ActionBar actionBar = getActionBar();
        if (actionBar !=null ) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mProgressBarLayout = (LinearLayout) findViewById(R.id.progressBarLayout);
        mGetRegisterCatalogBtn = (Button) findViewById(R.id.getRegisterCatalogBtn);

        getCatalogStatus = (TextView) findViewById(R.id.getCatalogStatus);

        mGetRegisterCatalogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsCatalogLoaded = false;
                // clear text view
                mConsoleText.setText("");
                try {
                    mProductService.getRegisterCatalogWithProducts(mProdCatWithProdListener);
                    showProgressBar();
                    mGetRegisterCatalogBtn.setEnabled(false);
                    new Thread(new Runnable() {
                        public void run() {
                            mProgressStatus = 0;
                            while (mProgressStatus < 100) {
                                mProgressStatus = getProgressStatus(mProgressStatus);

                                // Update the progress bar
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        mProgress.setProgress(mProgressStatus);
                                    }
                                });
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        mConsoleText = (TextView) findViewById(R.id.consoleText);

        mCreateProductBtn = (Button) findViewById(R.id.createProductBtn);
        mCreateProductBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Product newProduct = Util.createProduct();
                Gson gson = new Gson();
                Type productType = new TypeToken<Product>(){}.getType();
                System.out.println(gson.toJson(newProduct, productType));

                //TODO currently not implemented
                // productService.createProduct
                Toast.makeText(ProductServiceActivity.this, "Currently not implemented", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_PRODUCT_SERVICE),
                productServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(productServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_product, menu);
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

        if (id==android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showProgressBar(){
        mProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        mProgress.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
        mProgressBarLayout.addView(mProgress);
    }

    private void setStatus(final TextView textView, final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(msg);
            }
        });
    }
}
