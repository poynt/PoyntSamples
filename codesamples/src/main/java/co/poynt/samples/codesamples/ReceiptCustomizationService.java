package co.poynt.samples.codesamples;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.services.v1.IPoyntReceiptDecoratorService;
import co.poynt.os.services.v1.IPoyntReceiptDecoratorServiceListener;

/**
 * Created by dennis on 8/27/17.
 */

public class ReceiptCustomizationService extends Service {

    private static final String TAG = ReceiptCustomizationService.class.getSimpleName();

    private IPoyntReceiptDecoratorService.Stub mBinder = new IPoyntReceiptDecoratorService.Stub() {
        @Override
        public void decorate(PrintedReceipt printedReceipt, String requestId, IPoyntReceiptDecoratorServiceListener
                listener) throws RemoteException {
            List<PrintedReceiptLine> footerLines  = printedReceipt.getFooter();
            if (footerLines == null){
                footerLines = new ArrayList<PrintedReceiptLine>();
            }
            PrintedReceiptLine EMPTY_LINE = new PrintedReceiptLine();
            EMPTY_LINE.setText("\n");

            PrintedReceiptLine twitterInfo = new PrintedReceiptLine();
            twitterInfo.setText("We are on twitter @poynt");

            footerLines.add(EMPTY_LINE);
            footerLines.add(twitterInfo);
            footerLines.add(EMPTY_LINE);
            footerLines.add(EMPTY_LINE);
            footerLines.add(EMPTY_LINE);

            listener.onReceiptDecorated(requestId, printedReceipt);

        }

        @Override
        public void cancel(String requestId) throws RemoteException {

        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}