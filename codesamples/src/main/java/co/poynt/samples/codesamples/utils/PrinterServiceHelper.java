package co.poynt.samples.codesamples.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import co.poynt.os.model.AccessoryProvider;
import co.poynt.os.model.AccessoryProviderFilter;
import co.poynt.os.model.AccessoryType;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.model.PrinterStatus;
import co.poynt.os.services.v1.IPoyntAccessoryManager;
import co.poynt.os.services.v1.IPoyntAccessoryManagerListener;
import co.poynt.os.services.v1.IPoyntPrinterServiceListener;

public class PrinterServiceHelper {
    private static final String TAG = PrinterServiceHelper.class.getSimpleName();
    private Context context;
    private PrinterCallback callback;

    private IPoyntAccessoryManager mAccessoryManager;
    private HashMap<AccessoryProvider, IBinder> mPrinterServices = new HashMap<>();
    private ArrayList<ServiceConnection> printerConnections = new ArrayList<>();

    public PrinterServiceHelper(Context context, PrinterCallback callback) {
        this.context = context;
        bindAccessoryManager();
        this.callback = callback;
    }

    public HashMap<AccessoryProvider, IBinder> getPrinters() {
        return mPrinterServices;
    }

    public interface PrinterCallback {
        void onPrinterResponse(PrinterStatus status);

        void onPrinterReconnect(IBinder binder);

        void logMessage(String s);
    }

    public void bindAccessoryManager() {
        context.bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_ACCESSORY_SERVICE),
                accessoryService, Context.BIND_AUTO_CREATE);
    }


    public void unBindServices() {
        context.unbindService(accessoryService);
        unBindPrinters();
    }

    public void unBindPrinters() {
        for (ServiceConnection connection : printerConnections) {
            if (connection != null) {
                context.unbindService(connection);
            }
        }
        printerConnections.clear();
    }

    public void refreshPrinters() {
        printerConnections.clear();
        mPrinterServices.clear();
        callback.logMessage("Refreshing printers");
        try {
            mAccessoryManager.getAccessoryProviders(new AccessoryProviderFilter(AccessoryType.PRINTER), mAccessoryManagerListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public IPoyntPrinterServiceListener printerServiceListener = new IPoyntPrinterServiceListener.Stub() {
        @Override
        public void onPrintResponse(PrinterStatus printerStatus, String s) throws RemoteException {
            callback.onPrinterResponse(printerStatus);
        }
    };

    private ServiceConnection accessoryService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mAccessoryManager = IPoyntAccessoryManager.Stub.asInterface(service);
            Log.d(TAG, "Connected to accessory service");
            try {
                mAccessoryManager.getAccessoryProviders(new AccessoryProviderFilter(AccessoryType.PRINTER), mAccessoryManagerListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mAccessoryManager = null;
            Log.d(TAG, "Disconnected from accessory service");
        }
    };

    private IPoyntAccessoryManagerListener mAccessoryManagerListener = new IPoyntAccessoryManagerListener.Stub() {
        @Override
        public void onError(PoyntError poyntError) throws RemoteException {
            Log.d(TAG, "Failed to receive printers from accessory manager");
        }

        @Override
        public void onSuccess(List<AccessoryProvider> list) throws RemoteException {
            Log.d(TAG, "Received connected printers from accessory manager");

            for (AccessoryProvider printer : list) {
                String packageName = printer.getPackageName();
                if (printer.getClassName().contains("PoyntPrinterService")) {
                    packageName = "co.poynt.services";
                }
                Intent customIntent = new Intent();
                PrinterServiceConnection connection = new PrinterServiceConnection(printer, null);
                customIntent.setAction(AccessoryType.PRINTER.type());
                customIntent.setClassName(packageName, printer.getClassName());
                context.bindService(customIntent, connection, Context.BIND_AUTO_CREATE);
                printerConnections.add(connection);
            }
        }
    };

    public IBinder reconnectPrinter(AccessoryProvider printer) {
        String packageName = printer.getPackageName();
        if (printer.getClassName().contains("PoyntPrinterService")) {
            packageName = "co.poynt.services";
        }
        mPrinterServices.remove(printer);
        Intent customIntent = new Intent();
        PrinterServiceConnection connection = new PrinterServiceConnection(printer, callback);
        customIntent.setAction(AccessoryType.PRINTER.type());
        customIntent.setClassName(packageName, printer.getClassName());
        context.bindService(customIntent, connection, Context.BIND_AUTO_CREATE);
        printerConnections.add(connection);
        return connection.getService();
    }

    private class PrinterServiceConnection implements ServiceConnection {
        private AccessoryProvider provider;
        private IBinder service;
        private PrinterCallback callback;

        PrinterServiceConnection(AccessoryProvider provider, PrinterCallback callback) {
            this.provider = provider;
            this.callback = callback;
        }

        private IBinder getService() {
            return service;
        }

        private void resetService() {
            service = null;
        }

        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, " Accessory Service is now connected for " + provider.toString());
            this.service = service;
            mPrinterServices.put(provider, service);
            if (callback != null) {
                callback.onPrinterReconnect(service);
            }
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Printer connection disconnected unexpectedly"
                    + provider.toString());
            service = null;
            reconnectPrinter(provider);
        }
    }


}
