package co.poynt.samples.codesamples;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import co.poynt.os.Constants;

public class LoginActivity extends Activity {

    private AccountManager accountManager;
    private static final String TAG = LoginActivity.class.getName();
    private TextView userView;
    private TextView consoleText;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ActionBar actionBar = getActionBar();
        if (actionBar !=null) {
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
        if (id==android.R.id.home) {
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
                }else{
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

    private void displayAccessTokenInfo(String accessToken){
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
}
