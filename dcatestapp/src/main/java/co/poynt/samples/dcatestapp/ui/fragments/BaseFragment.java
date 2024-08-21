package co.poynt.samples.dcatestapp.ui.fragments;

import android.app.ProgressDialog;
import android.os.RemoteException;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import co.poynt.os.model.APDUData;
import co.poynt.os.model.ConnectionOptions;
import co.poynt.os.model.ConnectionResult;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntCardReaderService;
import co.poynt.os.services.v1.IPoyntConfigurationService;
import co.poynt.os.services.v1.IPoyntConnectToCardListener;
import co.poynt.os.services.v1.IPoyntDisconnectFromCardListener;
import co.poynt.os.services.v1.IPoyntExchangeAPDUListListener;
import co.poynt.os.services.v1.IPoyntExchangeAPDUListener;
import co.poynt.os.util.StringUtil;
import co.poynt.samples.dcatestapp.utils.IHelper;
import io.reactivex.Observable;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.CompositeDisposable;

public class BaseFragment extends Fragment {

    protected final CompositeDisposable compositeDisposable = new CompositeDisposable();
    
    protected final IHelper helper;

    protected BaseFragment(IHelper helper) {
        this.helper = helper;
    }

    protected IPoyntCardReaderService getCardReaderService() {
        return helper.getCardReaderService();
    }

    protected IPoyntConfigurationService getPoyntConfigurationService() {
        return helper.getPoyntConfigurationService();
    }

    protected void logReceivedMessage(String message) {
        helper.logReceivedMessage(message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.clear(); // Disposes of all added disposables
    }

    //region common methods
    // ---------------------------------------------------------------------------------------
    protected void connectToCard() {
        ConnectionOptions connectionOptions = createConnectionOptions(
                ConnectionOptions.ContactInterfaceType.EMV);
        logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
        try {
            getCardReaderService().connectToCard(connectionOptions,
                    new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage(connectionResult.toString());
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage(poyntError.toString());
                        }
                    });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    protected void disconnectFromCard() {
        disconnectFromCard(createConnectionOptions());
    }

    protected void disconnectFromCard(ConnectionOptions connectionOptions) {
        logReceivedMessage("disconnectFromCard : connectionOptions" + connectionOptions);

        final ProgressDialog cardRemovalProgress = new ProgressDialog(getContext());
        cardRemovalProgress.setMessage("waiting for card removal");
        cardRemovalProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        cardRemovalProgress.setIndeterminate(true);
        cardRemovalProgress.show();

        try {
            getCardReaderService().disconnectFromCard(connectionOptions, new IPoyntDisconnectFromCardListener.Stub() {
                @Override
                public void onDisconnect() throws RemoteException {
                    logReceivedMessage("Disconnected");
                    cardRemovalProgress.dismiss();
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Disconnection failed " + poyntError.toString());
                    cardRemovalProgress.dismiss();
                }
            });
        } catch (RemoteException e) {
            cardRemovalProgress.dismiss();
            throw new RuntimeException(e);
        }
    }

    protected Observable<ConnectionResult> connectToCardObservable() {
        return Observable.create(emitter -> {
            ConnectionOptions connectionOptions = createConnectionOptions();
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    emitter.onNext(connectionResult);
                }

                @Override
                public void onError(PoyntError poyntError) {
                    logReceivedMessage("Connection failure: " + poyntError.toString());
                    emitter.onError(new Throwable(poyntError.toString()));
                }
            });
        });
    }

    protected Observable<String> disconnectFromCardObservable() {
        return Observable.create(emitter -> {
            ConnectionOptions connectionOptions = createConnectionOptions();
            getCardReaderService().disconnectFromCard(connectionOptions, new IPoyntDisconnectFromCardListener.Stub() {
                @Override
                public void onDisconnect() {
                    logReceivedMessage("Disconnect from card: Success");
                    emitter.onNext("Success");
                }

                @Override
                public void onError(PoyntError poyntError) {
                    logReceivedMessage("Disconnect from card Failure: " + poyntError.toString());
                    emitter.onError(new Throwable(poyntError.toString()));
                }
            });
        });
    }

    protected Observable<String> exchangeAPDUObservable(final APDUData apduData) {
        return Observable.create(emitter -> getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
            @Override
            public void onSuccess(String responseAPDUData) throws RemoteException {
                logReceivedMessage("Exchange APDU result : " + responseAPDUData);

                if (responseAPDUData == null || responseAPDUData.length() < 4) {
                    logReceivedMessage("Exchange APDU Failed -> incorrect response length");
                    emitter.onError(new Throwable("incorrect response length"));
                    return;
                }

                String sw1sw2 = responseAPDUData.substring(responseAPDUData.length() - 4);
                if (!StringUtil.isEmpty(apduData.getOkCondition()) && !apduData.getOkCondition().contains(sw1sw2)) {
                    logReceivedMessage("Exchange APDU Failed -> sw1sw2 incorrect");
                    emitter.onError(new Throwable("sw1sw2 incorrect"));
                    return;
                }

                emitter.onNext(responseAPDUData);
            }

            @Override
            public void onError(PoyntError poyntError) throws RemoteException {
                logReceivedMessage("Exchange APDU Failed -> " + poyntError);
                emitter.onError(new Throwable(poyntError.toString()));
            }
        }));
    }

    protected Observable<List<String>> exchangeAPDUListObservable(final List<APDUData> apduData,
                                                                String testDescription,
                                                                boolean isCommandShouldFail) {
        return exchangeAPDUListObservable(apduData, testDescription, isCommandShouldFail, false);
    }

    protected Observable<List<String>> exchangeAPDUListObservable(final List<APDUData> apduData,
                                                                String testDescription,
                                                                boolean isCommandShouldFail,
                                                                boolean returnFullResponse) {
        return Observable.create(emitter -> getCardReaderService().exchangeAPDUList(apduData, new IPoyntExchangeAPDUListListener.Stub() {
            @Override
            public void onResult(List<String> list, PoyntError poyntError) {
                if (list != null) {
                    logReceivedMessage("Exchange APDU result : " + list.toString());
                }

                if (poyntError != null) {
                    if (isCommandShouldFail) {
                        logReceivedMessage(testDescription + " Failed, Test Passed");
                        emitter.onNext(list);
                    } else {
                        logReceivedMessage(testDescription + ": Failed -> " + poyntError.toString());
                        emitter.onError(new Throwable(poyntError.toString()));
                    }
                } else {
                    if (returnFullResponse) {
                        emitter.onNext(list);
                        return;
                    }

                    String rApdu = list.size() > 0 ? list.get(0) : null;

                    if (rApdu != null) {
                        if (rApdu.endsWith("9000")) {
                            logReceivedMessage(testDescription + ": Success");
                            list.set(0, list.get(0).substring(0, list.get(0).length() - 4));
                            emitter.onNext(list);
                        } else {
                            String errorMessage = "Unknown error";
                            if (rApdu.endsWith("6300")) {
                                errorMessage = "Authentication Failed";
                            } else if (rApdu.endsWith("6501")) {
                                errorMessage = "Memory failure, unable to read/write";
                            } else if (rApdu.endsWith("6502")) {
                                errorMessage = "Not a valid value block";
                            } else if (rApdu.endsWith("6600")) {
                                errorMessage = "Incorrect address range error";
                            } else if (rApdu.endsWith("6601")) {
                                errorMessage = "Incorrect length error";
                            } else if (rApdu.endsWith("6D00")) {
                                errorMessage = "Command not allowed";
                            }
                            if (isCommandShouldFail) {
                                logReceivedMessage(testDescription + " Failed with " + errorMessage + ", Test Passed");
                                emitter.onNext(list);
                            } else {
                                logReceivedMessage(testDescription + ": " + errorMessage);
                                emitter.onError(new Throwable(errorMessage));
                            }
                        }
                    } else {
                        logReceivedMessage(testDescription + ": Failed -> data is null");
                    }
                }
            }
        }));
    }

    protected ArrayList<APDUData> generateApduList(int testNumber, String logMessage, String... apduCommands) {
        ArrayList<APDUData> apduList = new ArrayList<>();

        for (String commandApdu : apduCommands) {
            APDUData apduData = createAPDUData(commandApdu);
            apduList.add(apduData);
            logReceivedMessage("Process Test" + testNumber + " - APDU Exchange - " + logMessage + " " + apduData);
        }

        return apduList;
    }
    // ---------------------------------------------------------------------------------------
    //endregion

    //region APDUData and ConnectionOptions creation
    // ---------------------------------------------------------------------------------------
    protected APDUData createAPDUData(String commandAPDU) {
        return createAPDUData(commandAPDU, null, null, 30);
    }

    protected APDUData createAPDUData(String commandAPDU,
                                      @Nullable APDUData.ContactInterfaceType contactInterfaceType) {
        return createAPDUData(commandAPDU, null, contactInterfaceType, 30);
    }

    protected APDUData createAPDUData(String commandAPDU,
                                      @Nullable String okCondition) {
        return createAPDUData(commandAPDU, okCondition, null, 30);
    }

    protected APDUData createAPDUData(String commandAPDU,
                                      @Nullable String okCondition,
                                      @Nullable APDUData.ContactInterfaceType contactInterfaceType) {
        return createAPDUData(commandAPDU, okCondition, contactInterfaceType, 30);
    }

    protected APDUData createAPDUData(String commandAPDU,
                                      @Nullable String okCondition,
                                      @Nullable APDUData.ContactInterfaceType contactInterfaceType,
                                      long timeout) {
        APDUData apduData = new APDUData();
        apduData.setCommandAPDU(commandAPDU);
        apduData.setOkCondition(okCondition);
        apduData.setContactInterface(contactInterfaceType);
        apduData.setTimeout(timeout);
        helper.updateAPDUDataInterface(apduData);
        return apduData;
    }
    // ---------------------------------------------------------------------------------------
    //endregion

    //region ConnectionOptions creation
    // ---------------------------------------------------------------------------------------
    protected ConnectionOptions createConnectionOptions() {
        return createConnectionOptions(null, 30);
    }

    protected ConnectionOptions createConnectionOptions(
            ConnectionOptions.ContactInterfaceType contactInterfaceType) {
        return createConnectionOptions(contactInterfaceType, 30);
    }

    protected ConnectionOptions createConnectionOptions(
            ConnectionOptions.ContactInterfaceType contactInterfaceType,
            int timeout) {
        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setContactInterface(contactInterfaceType);
        connectionOptions.setTimeout(timeout);
        helper.updateConnectionOptionsInterface(connectionOptions);
        return connectionOptions;
    }
    // ---------------------------------------------------------------------------------------
    //endregion
}
