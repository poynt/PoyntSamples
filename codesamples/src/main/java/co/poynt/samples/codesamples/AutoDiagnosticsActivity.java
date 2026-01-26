package co.poynt.samples.codesamples;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import co.poynt.os.Constants;
import co.poynt.os.model.DiagnosticsRequest;
import co.poynt.os.model.DiagnosticsResponse;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntConfigurationService;
import co.poynt.os.services.v1.IPoyntDeviceInfoListener;
import co.poynt.os.services.v1.IPoyntReaderVersionListener;
import co.poynt.os.services.v1.IPoyntSimCardInfoListener;
import co.poynt.os.services.v2.IPoyntDiagnosticsInfoListener;
import co.poynt.os.services.v2.IPoyntDiagnosticsInfoService;

public class AutoDiagnosticsActivity extends Activity {

    private static final String TAG = "AutoDiagnosticsActivity";

    private IPoyntDiagnosticsInfoService diagnosticsInfoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autodiagnostics);

        Button hardwareInfoButton = findViewById(R.id.getHardwareInfoButton);
        Button printerInfoButton = findViewById(R.id.getPrinterInfoButton);
        Button dockInfoButton = findViewById(R.id.getDockInfoButton);
        Button subscribeButton = findViewById(R.id.subscribeButton);
        Button unsubscribeButton = findViewById(R.id.unsubscribeButton);
        TextView infoTextView = findViewById(R.id.infoTextView);

        hardwareInfoButton.setOnClickListener(view -> {
            Log.d(TAG, "hardwareInfoButton is pressed!");
            try {
                Bundle request = DiagnosticsRequest
                        .create(DiagnosticsRequest.RequestType.HARDWARE_INFO)
                        .toBundle();
                diagnosticsInfoService.getDiagnosticsInfo(request, new IPoyntDiagnosticsInfoListener.Stub() {
                    @Override
                    public void onChanged(Bundle previousStateBundle, Bundle currentStateBundle) throws RemoteException {
                        DiagnosticsResponse response = DiagnosticsResponse.fromBundle(currentStateBundle);

                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("PhysicalKeyboardStatus: ")
                                .append(response.getPhysicalKeyboardStatus().name())
                                .append("\nChipReaderStatus: ")
                                .append(response.getChipReaderStatus().name())
                                .append("\nContactlessReaderStatus: ")
                                .append(response.getContactlessReaderStatus().name())
                                .append("\nMsrReaderStatus: ")
                                .append(response.getMsrReaderStatus().name());

                        runOnUiThread(() -> infoTextView.setText(stringBuilder));
                    }
                });
            } catch (Exception e) {
                infoTextView.setText("Exception: " + e.getMessage());
            }
        });

        printerInfoButton.setOnClickListener(view -> {
            Log.d(TAG, "printerInfoButton is pressed!");
            try {
                Bundle request = DiagnosticsRequest
                        .create(DiagnosticsRequest.RequestType.PRINTER_INFO)
                        .toBundle();
                diagnosticsInfoService.getDiagnosticsInfo(request, new IPoyntDiagnosticsInfoListener.Stub() {
                    @Override
                    public void onChanged(Bundle previousStateBundle, Bundle currentStateBundle) throws RemoteException {
                        DiagnosticsResponse response = DiagnosticsResponse.fromBundle(currentStateBundle);

                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("PrinterState: ")
                                .append(response.getPrinterState().name());

                        runOnUiThread(() -> infoTextView.setText(stringBuilder));
                    }
                });
            } catch (Exception e) {
                infoTextView.setText("Exception: " + e.getMessage());
            }
        });

        dockInfoButton.setOnClickListener(view -> {
            Log.d(TAG, "dockInfoButton is pressed!");
            try {
                Bundle request = DiagnosticsRequest
                        .create(DiagnosticsRequest.RequestType.DOCK_STATION_INFO)
                        .toBundle();
                diagnosticsInfoService.getDiagnosticsInfo(request, new IPoyntDiagnosticsInfoListener.Stub() {
                    @Override
                    public void onChanged(Bundle previousStateBundle, Bundle currentStateBundle) throws RemoteException {
                        DiagnosticsResponse response = DiagnosticsResponse.fromBundle(currentStateBundle);

                        Log.d(TAG, "getting dock info, response.isConnectedToDock() = " + response.isConnectedToDock());

                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("isConnectedToDock: ")
                                .append(response.isConnectedToDock())
                                .append("\nhasLanWiredConnection: ")
                                .append(response.hasLanWiredConnection())
                                .append("\nisLanConnectionAvailable: ")
                                .append(response.isLanConnectionAvailable())
                                .append("\nLAN IP Adress: ")
                                .append(response.getLanConnectionIpAddress());

                        runOnUiThread(() -> infoTextView.setText(stringBuilder));
                    }
                });
            } catch (Exception e) {
                infoTextView.setText("Exception: " + e.getMessage());
            }
        });

        subscribeButton.setOnClickListener(view -> {
            try {
                Log.d(TAG, "subscribeButton is pressed!");
                Bundle request = DiagnosticsRequest
                        .create(DiagnosticsRequest.RequestType.HARDWARE_INFO,
                                DiagnosticsRequest.RequestType.PRINTER_INFO,
                                DiagnosticsRequest.RequestType.DOCK_STATION_INFO)
                        .toBundle();
                diagnosticsInfoService.subscribe(request, new IPoyntDiagnosticsInfoListener.Stub() {
                    @Override
                    public void onChanged(Bundle previousStateBundle, Bundle currentStateBundle) throws RemoteException {
                        DiagnosticsResponse response = DiagnosticsResponse.fromBundle(currentStateBundle);

                        Log.d(TAG, "onChanged! response.isConnectedToDock() = " + response.isConnectedToDock());

                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Diagnostics Info is Changed!")
                                .append("\nPrinter: ")
                                .append(response.getPrinterState().name())
                                .append("\nPhysicalKeyboardStatus: ")
                                .append(response.getPhysicalKeyboardStatus().name())
                                .append("\nChipReaderStatus: ")
                                .append(response.getChipReaderStatus().name())
                                .append("\nContactlessReaderStatus: ")
                                .append(response.getContactlessReaderStatus().name())
                                .append("\nMsrReaderStatus: ")
                                .append(response.getMsrReaderStatus().name())
                                .append("\nisConnectedToDock: ")
                                .append(response.isConnectedToDock())
                                .append("\nhasLanWiredConnection: ")
                                .append(response.hasLanWiredConnection())
                                .append("\nisLanConnectionAvailable: ")
                                .append(response.isLanConnectionAvailable())
                                .append("\nLAN IP Adress: ")
                                .append(response.getLanConnectionIpAddress())
                        ;

                        runOnUiThread(() -> infoTextView.setText(stringBuilder));
                    }
                });
            } catch (Exception e) {
                infoTextView.setText("Exception: " + e.getMessage());
            }
        });

        unsubscribeButton.setOnClickListener(view -> {
            try {
                infoTextView.setText("Unsubscribed");
                Bundle request = DiagnosticsRequest
                        .create(DiagnosticsRequest.RequestType.HARDWARE_INFO,
                                DiagnosticsRequest.RequestType.PRINTER_INFO,
                                DiagnosticsRequest.RequestType.DOCK_STATION_INFO)
                        .toBundle();
                diagnosticsInfoService.unsubscribe(request, new IPoyntDiagnosticsInfoListener.Stub() {
                    @Override
                    public void onChanged(Bundle previousStateBundle, Bundle currentStateBundle) {

                    }
                });
            } catch (Exception e) {
                infoTextView.setText("Exception: " + e.getMessage());
            }
        });

    }

    private Intent createExplicitServiceIntent() {
        PackageManager pm = getPackageManager();
        Intent implicitIntent = new Intent(Constants.DiagnosticsInfoRequest.ACTION);
        List<ResolveInfo> services = pm.queryIntentServices(implicitIntent, 0);
        if (services == null || services.isEmpty()) {
            Log.e(TAG, "No diagnostics service found");
            return null;
        }

        ResolveInfo serviceInfo = services.get(0);
        Intent explicitIntent = new Intent();
        explicitIntent.setComponent(
                new ComponentName(serviceInfo.serviceInfo.packageName, serviceInfo.serviceInfo.name));
        Log.d(TAG, "Found service: " + serviceInfo.serviceInfo.packageName);
        return explicitIntent;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "binding to service...");
        final Intent intent = createExplicitServiceIntent();
        bindService(intent, diagnosticsServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "unbinding from service...");
        unbindService(diagnosticsServiceConnection);
    }

    private ServiceConnection diagnosticsServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "IPoyntDiagnosticsInfoService is now connected");
            diagnosticsInfoService = IPoyntDiagnosticsInfoService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "IPoyntDiagnosticsInfoService has unexpectedly disconnected");
            diagnosticsInfoService = null;
        }
    };

}
