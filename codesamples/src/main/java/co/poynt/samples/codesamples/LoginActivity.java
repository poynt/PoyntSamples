package co.poynt.samples.codesamples;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import co.poynt.os.Constants;

import static co.poynt.os.Constants.Accounts.POYNT_ACCOUNT_TYPE;

public class LoginActivity extends Activity {

    private static final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 84657;
    private AccountManager accountManager;
    private static final String TAG = LoginActivity.class.getName();
    private TextView userView;
    private TextView consoleText;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        accountManager = AccountManager.get(this);
        userView = (TextView) findViewById(R.id.userTextView);
        consoleText = (TextView) findViewById(R.id.consoleText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
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

    public void onLoginClicked(View view) {
        Log.d(TAG, "onLoginClicked called");
        accountManager.getAuthToken(Constants.Accounts.POYNT_UNKNOWN_ACCOUNT,
                Constants.Accounts.POYNT_AUTH_TOKEN, null, LoginActivity.this,
                new OnUserLoginAttempt(), null);
    }

    public class OnUserLoginAttempt implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
            try {
                Bundle bundle = accountManagerFuture.getResult();
                String user = (String) bundle.get(AccountManager.KEY_ACCOUNT_NAME);
                String authToken = (String) bundle.get(AccountManager.KEY_AUTHTOKEN);
                if (authToken != null) {
                    // Log.d(TAG, "authtoken: " + authToken);
                    displayAccessTokenInfo(authToken);
                    Toast.makeText(LoginActivity.this, "User " + user + " successfully logged in", Toast.LENGTH_LONG).show();
                    userView.setText(user);
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                }
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                Toast.makeText(LoginActivity.this, "Login canceled", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            }
        }
    }

    private void displayAccessTokenInfo(String accessToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(accessToken);

            StringBuilder claimsBuffer = new StringBuilder();
            ReadOnlyJWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            claimsBuffer.append("Subject: " + claims.getSubject())
                    .append("\nType: " + claims.getType())
                    .append("\nIssuer: " + claims.getIssuer())
                    .append("\nJWT ID: " + claims.getJWTID())
                    .append("\nIssueTime : " + claims.getIssueTime())
                    .append("\nExpiration Time: " + claims.getExpirationTime())
                    .append("\nNot Before Time: " + claims.getNotBeforeTime());
            for (String audience : claims.getAudience()) {
                claimsBuffer.append("\nAudience: " + audience);
            }

            Map<String, Object> customClaims = claims.getCustomClaims();
            for (Map.Entry<String, Object> entry : customClaims.entrySet()) {
                String key = entry.getKey();
                switch (key) {
                    case "poynt.did":
                        key += " (Device ID)";
                        break;
                    case "poynt.biz":
                        key += " (Business ID)";
                        break;
                    case "poynt.ist":
                        key += " (Issued To)";
                        break;
                    case "poynt.sct":
                        key += " (Subject Credential Type [J=JWT, E=EMAIL, U=USERNAME])";
                        break;
                    case "poynt.str":
                        key += " (Store ID)";
                        break;
                    case "poynt.kid":
                        key += " (Key ID)";
                        break;
                    case "poynt.ure":
                        key += " (User Role [O=Owner, E=Employee])";
                        break;
                    case "poynt.uid":
                        key += " (Poynt User ID)";
                        break;
                    case "poynt.scv":
                        key += " (Subject Credential Value)";
                        break;
                    default:
                        break;
                }

                claimsBuffer.append("\n" + key + ": " + entry.getValue());
            }
            final String claimsStr = claimsBuffer.toString();
            Log.d(TAG, "claims: " + claimsStr);
            consoleText.setText(claimsStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void onChallengePINClicked(View view) {
        Log.d(TAG, "onChallengePINClicked called");
        Account account = new Account("Yes", Constants.Accounts.POYNT_ACCOUNT_TYPE);
        accountManager.getAuthToken(account,
                Constants.Accounts.POYNT_AUTH_TOKEN, null, LoginActivity.this,
                new OnUserLoginAttempt(), null);
    }

    public void onGetAllUsersClicked(View view) {
        Log.d(TAG, "onGetAllUsersClicked called");
        getAccounts();
    }

    private void getAccounts() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    consoleText.setText("No permission to get accounts - requesting...!");
                }
            });


            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.GET_ACCOUNTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.GET_ACCOUNTS},
                        MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

            return;
        }
        Account[] accounts = accountManager.getAccountsByType(POYNT_ACCOUNT_TYPE);
        if (accounts != null) {
            final StringBuilder accountsList = new StringBuilder();
            for (Account account : accounts) {
                accountsList.append("\n");
                accountsList.append(account.name);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    consoleText.setText(accountsList.toString());
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_GET_ACCOUNTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    getAccounts();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
