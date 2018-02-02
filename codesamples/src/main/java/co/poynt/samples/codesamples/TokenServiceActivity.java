package co.poynt.samples.codesamples;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.os.AsyncTask;
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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Map;
import java.util.Properties;

import co.poynt.api.model.TokenResponse;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntTokenService;
import co.poynt.os.services.v1.IPoyntTokenServiceListener;
import co.poynt.samples.codesamples.utils.Util;

public class TokenServiceActivity extends Activity {
    private static final String TAG = TokenServiceActivity.class.getName();
    // the app id issued to you when you upload the apk to poynt.net
    // add it to src/main/assets/config.properties (on Android Studio)
    private String appId;
    private Button getTokenBtn;
    private Button verifyJwtBtn;
    private TextView textView;
    private TextView getTokenStatus;
    private TextView verifyJwtStatus;
    // public certified for services.poynt.net
    private X509Certificate poyntCert;
    private SignedJWT signedJWT;

    private IPoyntTokenService tokenService;

    private ServiceConnection tokenServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            tokenService = IPoyntTokenService.Stub.asInterface(iBinder);
            Log.d(TAG, "onServiceConnected ");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected ");
        }
    };

    private IPoyntTokenServiceListener tokenServiceListener = new IPoyntTokenServiceListener.Stub() {
        @Override
        public void onResponse(TokenResponse tokenResponse, PoyntError poyntError) throws RemoteException {
            if (tokenResponse != null) {
                Log.d(TAG, "onResponse " + tokenResponse.getAccessToken());
                String accessToken = tokenResponse.getAccessToken();
                try {
                    signedJWT = SignedJWT.parse(accessToken);

                    // JWSVerifier verifier
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
                            default:
                                break;
                        }

                        claimsBuffer.append("\n" + key + ": " + entry.getValue());
                    }
                    final String claimsStr = claimsBuffer.toString();
                    Log.d(TAG, "claims: " + claimsStr);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(claimsStr);
                            verifyJwtBtn.setEnabled(true);
                            getTokenStatus.setText("SUCCESS");
                        }
                    });

                } catch (ParseException e) {
                    e.printStackTrace();
                }

            } else if (poyntError != null) {
                Log.d(TAG, "onResponse error" + poyntError.getData());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_service);

        android.app.ActionBar actionBar = getActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        textView = (TextView) findViewById(R.id.consoleText);
        getTokenBtn = (Button) findViewById(R.id.getTokenBtn);
        getTokenBtn.setEnabled(false);

        verifyJwtBtn = (Button) findViewById(R.id.verifyJwtBtn);
        verifyJwtBtn.setEnabled(false);

        getTokenStatus = (TextView) findViewById(R.id.getTokenStatusText);
        verifyJwtStatus = (TextView) findViewById(R.id.verifyJwtStatusText);

        getTokenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tokenService != null) {
                    try {
                        tokenService.grantToken(appId, tokenServiceListener);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        verifyJwtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (poyntCert != null && signedJWT != null) {
                    JWSAlgorithm algorithm = signedJWT.getHeader().getAlgorithm();

                    if (algorithm.equals(JWSAlgorithm.RS256)) {
                        final JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) poyntCert.getPublicKey());
                        try {
                            final boolean isSignatureVerified = signedJWT.verify(verifier);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(TokenServiceActivity.this,
                                            "JWT signature verified: " + isSignatureVerified, Toast.LENGTH_SHORT).show();
                                    verifyJwtStatus.setText("SUCCESS");
                                }
                            });
                        } catch (JOSEException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        // download Poynt certificate to be used for JWT signature verification
        new GetCertTask().execute();
        loadConfig();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_TOKEN_SERVICE),
                tokenServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(tokenServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_token, menu);
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

    class GetCertTask extends AsyncTask<Void, Void, X509Certificate> {
        @Override
        protected X509Certificate doInBackground(Void... voids) {
            return Util.getPoyntCert();
        }

        @Override
        protected void onPostExecute(X509Certificate cert) {
            super.onPostExecute(cert);
            if (cert != null) {
                getTokenBtn.setEnabled(true);
                poyntCert = cert;
            } else {
                Toast.makeText(TokenServiceActivity.this, "Getting certificate failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
     * loads applications appId from a properties file which should be located in src/main/assets
     * File format:
     * appId=urn:aid:....
     * You can get the appId by uploading the first version of your apk from your dev account on poynt.net
     */
    private void loadConfig() {
        try {
            for (String s : getAssets().list(".")) {
                Log.d(TAG, "loadConfig " + s);
            }
            InputStream is = getAssets().open("config.properties");
            Properties props = new Properties();
            props.load(is);
            appId = props.getProperty("appId");
            Log.d(TAG, "loaded appId: " + appId.toString());
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please set \"appId\" property in src/main/assets/config.properties");
            builder.setTitle("Unable to initialize appId");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
}
