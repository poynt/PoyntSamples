package co.poynt.samples.codesamples;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntConfigurationService;
import co.poynt.os.services.v1.IPoyntDeviceInfoListener;
import co.poynt.os.services.v1.IPoyntReaderVersionListener;
import co.poynt.os.services.v1.IPoyntSimCardInfoListener;

public class ConfigurationServiceActivity extends Activity {

    private static final String TAG = "ConfigServiceActivity";

    private IPoyntConfigurationService configurationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration_service);

        Button getSimInfoButton = findViewById(R.id.getSimInfoButton);
        Button getFirmwareComponentVersBtn = findViewById(R.id.getFirmwareCompVersButton);
        Button getDeviceInfoButton = findViewById(R.id.getDeviceInfoButton);
        TextView infoTextView = findViewById(R.id.infoTextView);

        getSimInfoButton.setOnClickListener(view -> {
            try {
                configurationService.getSimCardInfo(new IPoyntSimCardInfoListener.Stub() {
                    @Override
                    public void onSimCardInfoReceived(Bundle bundle) throws RemoteException {
                        runOnUiThread(() -> {
                            StringBuilder stringBuilder = new StringBuilder("Sim Card Info:\n");
                            for (String key : bundle.keySet()) {
                                stringBuilder.append(key).append(" : ").append(bundle.get(key)).append("\n");
                            }
                            infoTextView.setText(stringBuilder);
                        });
                    }
                });
            } catch (Exception e) {
                infoTextView.setText("Exception: " + e.getMessage());
            }
        });

        getFirmwareComponentVersBtn.setOnClickListener(v -> {
            try {
                configurationService.getFirmwareComponentVersion(new IPoyntReaderVersionListener.Stub() {
                    @Override
                    public void onSuccess(String fwVersion) throws RemoteException {
                        runOnUiThread(() -> {
                            Log.d("FW Component version",
                                    fwVersion.replaceAll("(\\r|\\n)", ""));
                            infoTextView.setText(fwVersion);
                        });
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onFailure(PoyntError poyntError) throws RemoteException {
                        if (poyntError != null) {
                            runOnUiThread(() -> {
                                infoTextView.setText("Error: " + poyntError.toString());
                            });
                        }
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        getDeviceInfoButton.setOnClickListener(view -> {
            try {
                configurationService.getDeviceInfo(new IPoyntDeviceInfoListener.Stub() {

                    @Override
                    public void onDeviceInfoReceived(Bundle bundle) throws RemoteException {
                        runOnUiThread(() -> {
                            StringBuilder stringBuilder = new StringBuilder("Device Info:\n");
                            for (String key : bundle.keySet()) {
                                stringBuilder.append(key).append(" : ").append(bundle.get(key)).append("\n");
                            }
                            infoTextView.setText(stringBuilder);
                        });
                    }
                });
            } catch (Exception e) {
                infoTextView.setText("Exception: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "binding to service...");
        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_CONFIGURATION_SERVICE),
                configurationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "unbinding from service...");
        unbindService(configurationServiceConnection);
    }

    private ServiceConnection configurationServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "PoyntConfigurationService is now connected");
            configurationService = IPoyntConfigurationService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "PoyntConfigurationService has unexpectedly disconnected");
            configurationService = null;
        }
    };

}
