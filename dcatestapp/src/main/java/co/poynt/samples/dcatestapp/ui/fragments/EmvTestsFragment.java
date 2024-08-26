package co.poynt.samples.dcatestapp.ui.fragments;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import co.poynt.os.model.APDUData;
import co.poynt.os.model.ConnectionOptions;
import co.poynt.os.model.ConnectionResult;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntCardInsertListener;
import co.poynt.os.services.v1.IPoyntConnectToCardListener;
import co.poynt.os.services.v1.IPoyntExchangeAPDUListListener;
import co.poynt.os.services.v1.IPoyntExchangeAPDUListener;
import co.poynt.samples.dcatestapp.databinding.FragmentEmvTestsBinding;
import co.poynt.samples.dcatestapp.utils.IHelper;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class EmvTestsFragment extends BaseFragment {
    private FragmentEmvTestsBinding binding;

    public EmvTestsFragment(IHelper helper) {
        super(helper);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEmvTestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //CT EMV tests
        binding.ctSuccessfulTransaction.setOnClickListener(v -> ctSuccessTransaction());
        binding.ctFileNotFound.setOnClickListener(v -> ctFileNotFound());
        binding.ctCardRejectionMaster.setOnClickListener(v -> ctCardRejectionMaster());
        binding.ctExchangeApduList.setOnClickListener(v -> ctExchangeApdu());
        binding.ctPymtTransactionDuringDCA.setOnClickListener(v -> ctPymtTrnDuringDCA());
        binding.ctXAPDU.setOnClickListener(v -> ctXAPDU());

        //CL EMV tests
        binding.clSuccessfulTransaction.setOnClickListener(v -> clSuccessTransaction());
        binding.clFileNotFound.setOnClickListener(v -> clFileNotFound());
        binding.clCardRejectionMaster.setOnClickListener(v -> clCardRejectionMaster());
        binding.clExchangeApduList.setOnClickListener(v -> clExchangeApdu());
        binding.clPymtTransactionDuringDCA.setOnClickListener(v -> clPymtTrnDuringDCA());
        binding.clXAPDU.setOnClickListener(v -> clXAPDU());

        //ISO 7816(GSM) tests
        binding.isoSuccessfulTransaction.setOnClickListener(v -> isoSuccessfulTransaction());
        binding.isoFileNotFound.setOnClickListener(v -> isoFileNotFound());
        binding.isoCardRejectionMaster.setOnClickListener(v -> isoCardRejectionMaster());
        binding.isoExchangeApduList.setOnClickListener(v -> isoExchangeApdu());
        binding.isoTrnDuringDCA.setOnClickListener(v -> isoPymntTrnDuringDca());
        binding.isoXAPDU.setOnClickListener(v -> isoXAPDU());
        binding.isoItalianHealthCards.setOnClickListener(v -> isoItalianHealthCards());

        //SLE tests
        binding.sle401.setOnClickListener(v -> sle401());
        binding.sle402.setOnClickListener(v -> sle402());
        binding.sle403.setOnClickListener(v -> sle403());
        binding.sle404.setOnClickListener(v -> sle404());
        binding.sle405.setOnClickListener(v -> sle405());
        binding.sle406.setOnClickListener(v -> sle406());
        binding.sle407.setOnClickListener(v -> sle407());
        binding.sle408.setOnClickListener(v -> sle408());
        binding.sle409.setOnClickListener(v -> sle409());
        binding.sleXAPDU.setOnClickListener(v -> sleXAPDU());
    }

    //region CT tests
    //---------------------------------------------------------------------------------------
    private void ctSuccessTransaction() {
        logReceivedMessage("===============================");
        logReceivedMessage("Successful non-payment transaction Test");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.EMV);
            logReceivedMessage("checkIfCardInserted : ConnectionOptions : " + connectionOptions);
            getCardReaderService().checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {

                    logReceivedMessage("CardFound");
                    logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
                    getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success : connectionResult " + connectionResult);
                            APDUData apduData = createAPDUData(
                                    "0400A404000E315041592E5359532E444446303100",
                                    APDUData.ContactInterfaceType.EMV);
                            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                            getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    logReceivedMessage("Exchange APDU result : " + s);

                                    if (s.endsWith("9000")) {
                                        logReceivedMessage("Successful non-payment transaction Test Passed");
                                    } else {
                                        logReceivedMessage("Successful non-payment transaction Test Failed");
                                    }
                                    disconnectFromCard(connectionOptions);
                                }

                                @Override
                                public void onError(PoyntError poyntError) throws RemoteException {
                                    logReceivedMessage("APDU exchange failed " + poyntError);
                                    disconnectFromCard(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card Not Found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void ctFileNotFound() {
        logReceivedMessage("===============================");
        logReceivedMessage("File Not Found Test");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.EMV);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            getCardReaderService().checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");
                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
                    getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success : connectionResult " + connectionResult);
                            APDUData apduData = createAPDUData(
                                    "0400A404000A4F53452E5641532E303100",
                                    APDUData.ContactInterfaceType.EMV);
                            logReceivedMessage("exchangeAPDU apduData " + apduData);
                            getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    logReceivedMessage("Exchange APDU result : " + s);

                                    if (s.endsWith("6A82")) {
                                        logReceivedMessage("File not found Test Passed");
                                    } else {
                                        logReceivedMessage("APDU response " + s);
                                    }
                                    disconnectFromCard(connectionOptions);
                                }

                                @Override
                                public void onError(PoyntError poyntError) throws RemoteException {
                                    logReceivedMessage("APDU exchange failed " + poyntError);
                                    disconnectFromCard(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card Not Found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void ctCardRejectionMaster() {
        logReceivedMessage("===============================");
        logReceivedMessage("CT Payment Card Rejection Test - Master Card");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.EMV);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);
                    final APDUData apduData = createAPDUData(
                            "0400A404000E315041592E5359532E444446303100",
                            APDUData.ContactInterfaceType.EMV);
                    logReceivedMessage("exchangeAPDU : apdudData " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);
                            if (s.endsWith("9000")) {
                                APDUData apduData2 = createAPDUData(
                                        "0400A4040007A000000003101000",
                                        APDUData.ContactInterfaceType.EMV);
                                logReceivedMessage("exchangeAPDU : apdudData " + apduData2);
                                getCardReaderService().exchangeAPDU(apduData2, new IPoyntExchangeAPDUListener.Stub() {
                                    @Override
                                    public void onSuccess(String s) throws RemoteException {
                                        logReceivedMessage("Exchange APDU result : " + s);
                                        disconnectFromCard(connectionOptions);
                                    }

                                    @Override
                                    public void onError(PoyntError poyntError) throws RemoteException {
                                        logReceivedMessage("APDU exchange failed " + poyntError);
                                        logReceivedMessage("PaymentCard Rejection Success");
                                        disconnectFromCard(connectionOptions);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void ctExchangeApdu() {
        logReceivedMessage("===============================");
        logReceivedMessage("Exchange CT APDU Test");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.EMV);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            getCardReaderService().checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");
                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
                    getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success : connectionResult " + connectionResult);

                            List<APDUData> apduDataList = new ArrayList<>();

                            apduDataList.add(
                                    createAPDUData(
                                            "0400A404000E315041592E5359532E444446303100",
                                            APDUData.ContactInterfaceType.EMV));
                            apduDataList.add(
                                    createAPDUData(
                                            "0400A404000E325041592E5359532E444446303100",
                                            "6A816A82",
                                            APDUData.ContactInterfaceType.EMV));
                            apduDataList.add(
                                    createAPDUData(
                                            "0400A404000E315041592E5359532E444446303100",
                                            APDUData.ContactInterfaceType.EMV));

                            logReceivedMessage("exchangeAPDUList : apduData " + apduDataList);

                            getCardReaderService().exchangeAPDUList(apduDataList, new IPoyntExchangeAPDUListListener.Stub() {
                                @Override
                                public void onResult(List<String> list, PoyntError poyntError) throws RemoteException {
                                    if (poyntError != null) {
                                        logReceivedMessage("Exchange APDU List failed " + poyntError);
                                    } else {
                                        logReceivedMessage("Exchange APDU result : " + list.toString());
                                        if (list.size() >= 3) {
                                            if (list.get(0).endsWith("9000") && list.get(2).endsWith("9000")) {
                                                if (list.get(1).endsWith("9000") || list.get(1).endsWith("6A82")) {
                                                    logReceivedMessage("Exchange APDU list test passed");

                                                }
                                            }
                                        }
                                    }
                                    disconnectFromCard(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card not found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void ctPymtTrnDuringDCA() {
        logReceivedMessage("===============================");
        logReceivedMessage("104b Payment Transaction during DCA (PoyntC Only)");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.EMV);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);

                    final APDUData apduData = createAPDUData(
                            "0400A404000E315041592E5359532E444446303100",
                            APDUData.ContactInterfaceType.EMV);
                    logReceivedMessage("exchangeAPDU : apduData " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);
                            if (s.endsWith("9000")) {
                                logReceivedMessage("Now open the Terminal app and perform a Payment transaction, Transaction should be success");
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void ctXAPDU() {
        logReceivedMessage("===============================");
        logReceivedMessage("CT XAPDU");
        try {
            logReceivedMessage("Exchange APDU");
            APDUData apduData = createAPDUData(
                    "0400A404000E315041592E5359532E444446303100",
                    APDUData.ContactInterfaceType.EMV);
            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
            getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    logReceivedMessage("Exchange APDU result : " + s);
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("APDU exchange failed " + poyntError);
                    logReceivedMessage("Test passed");
                    final ConnectionOptions connectionOptions = createConnectionOptions(
                            ConnectionOptions.ContactInterfaceType.EMV);
                    logReceivedMessage("disconnectCardReader : ConnectionOptions : " + connectionOptions);
                    disconnectFromCard(connectionOptions);
                }
            });
        } catch (Exception e) {
            logReceivedMessage(e.getMessage());
        }
    }
    //---------------------------------------------------------------------------------------
    //endregion


    //region CL Tests
    //---------------------------------------------------------------------------------------
    private void clSuccessTransaction() {
        logReceivedMessage("===============================");
        logReceivedMessage("Successful non-payment transaction CL Test");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(null);
            logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);
                    APDUData apduData = createAPDUData(
                            "0400A404000E325041592E5359532E444446303100");
                    logReceivedMessage("exchangeAPDU : apuduData  " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("Successful non-payment CL transaction Test Passed");
                            } else {
                                logReceivedMessage("Successful non-payment CL transaction Test Failed");
                            }
                            disconnectFromCard(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    private void clFileNotFound() {
        logReceivedMessage("===============================");
        logReceivedMessage("File Not Found CL Test");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(null);
            logReceivedMessage("connectToCard : connectionOptions : " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);
                    APDUData apduData = createAPDUData(
                            "0400A404000A4F53452E5641532E303100");
                    logReceivedMessage("exchangeAPDU apduData " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("6A82")) {
                                logReceivedMessage("File not found CL Test Passed");
                            } else {
                                logReceivedMessage("APDU response " + s);
                            }
                            disconnectFromCard(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void clCardRejectionMaster() {
        logReceivedMessage("===============================");
        logReceivedMessage("CL Payment Card Rejection Test - Master Card");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(null);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);
                    final APDUData apduData = createAPDUData(
                            "0400A404000E325041592E5359532E444446303100");
                    logReceivedMessage("exchangeAPDU : apuduData " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);
                            if (s.endsWith("9000")) {
                                APDUData apduData2 = createAPDUData(
                                        "0400A4040007A000000003101000");
                                logReceivedMessage("exchangeAPDU : apduData " + apduData2);
                                getCardReaderService().exchangeAPDU(apduData2, new IPoyntExchangeAPDUListener.Stub() {
                                    @Override
                                    public void onSuccess(String s) throws RemoteException {
                                        logReceivedMessage("Exchange APDU result : " + s);
                                        disconnectFromCard(connectionOptions);
                                    }

                                    @Override
                                    public void onError(PoyntError poyntError) throws RemoteException {
                                        logReceivedMessage("APDU exchange failed " + poyntError);
                                        logReceivedMessage("PaymentCard Rejection Success");
                                        disconnectFromCard(connectionOptions);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void clExchangeApdu() {
        logReceivedMessage("===============================");
        logReceivedMessage("Exchange CL APDU Test");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(null);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);

                    List<APDUData> apduDataList = new ArrayList<>();
                    apduDataList.add(createAPDUData(
                            "0400A404000E325041592E5359532E444446303100"));
                    apduDataList.add(createAPDUData(
                            "0400A404000A4F53452E5641532E303100",
                            "6A82"));
                    apduDataList.add(createAPDUData(
                            "0400A404000E325041592E5359532E444446303100"));
                    logReceivedMessage("exchangeAPDUList : " + apduDataList);

                    getCardReaderService().exchangeAPDUList(apduDataList, new IPoyntExchangeAPDUListListener.Stub() {
                        @Override
                        public void onResult(List<String> list, PoyntError poyntError) throws RemoteException {
                            if (poyntError != null) {
                                logReceivedMessage("Exchange CL APDU List failed " + poyntError);
                            } else {
                                logReceivedMessage("Exchange APDU result : " + list.toString());
                                if (list.size() >= 2) {
                                    if (list.get(1).endsWith("6A82")) {
                                        logReceivedMessage("Exchange APDU list Test passed");
                                    }
                                }
                            }
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void clPymtTrnDuringDCA() {
        logReceivedMessage("===============================");
        logReceivedMessage("204b Payment Transaction during DCA (PoyntC Only)");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(null);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);

                    final APDUData apduData = createAPDUData(
                            "0400A404000E325041592E5359532E444446303100");
                    logReceivedMessage("exchangeAPDU : apduData " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);
                            if (s.endsWith("9000")) {
                                logReceivedMessage("Now open the Terminal app and perform a Payment transaction, Transaction should be success");
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void clXAPDU() {
        logReceivedMessage("===============================");
        logReceivedMessage("CL XAPDU");
        try {
            logReceivedMessage("Exchange APDU");
            APDUData apduData = createAPDUData(
                    "0400A404000E325041592E5359532E444446303100");
            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
            getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    logReceivedMessage("Exchange APDU result : " + s);
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("APDU exchange failed " + poyntError);
                    logReceivedMessage("Test passed");
                    final ConnectionOptions connectionOptions = createConnectionOptions(null);
                    logReceivedMessage("disconnectCardReader : ConnectionOptions : " + connectionOptions);
                    disconnectFromCard(connectionOptions);
                }
            });
        } catch (Exception e) {
            logReceivedMessage(e.getMessage());
        }
    }
    // ---------------------------------------------------------------------------------------
    //endregion

    //region ISO 7816 tests
    // ---------------------------------------------------------------------------------------
    private void isoSuccessfulTransaction() {
        logReceivedMessage("===============================");
        logReceivedMessage("Successful non-payment transaction Test");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.GSM);
            logReceivedMessage("checkIfCardInserted : ConnectionOptions : " + connectionOptions);
            getCardReaderService().checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {

                    logReceivedMessage("CardFound");
                    logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
                    getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                            APDUData apduData = createAPDUData(
                                    "0400A404000E315041592E5359532E444446303100",
                                    APDUData.ContactInterfaceType.GSM);
                            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                            getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    logReceivedMessage("Exchange APDU result : " + s);

                                    if (s.endsWith("9000") || s.endsWith("6A81") || s.endsWith("6A82")) {
                                        logReceivedMessage("Successful non-payment transaction Test Passed");
                                    } else {
                                        logReceivedMessage("Successful non-payment transaction Test Failed");
                                    }
                                    disconnectFromCard(connectionOptions);
                                }

                                @Override
                                public void onError(PoyntError poyntError) throws RemoteException {
                                    logReceivedMessage("APDU exchange failed " + poyntError);
                                    disconnectFromCard(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card Not Found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoFileNotFound() {
        logReceivedMessage("===============================");
        logReceivedMessage("File Not Found Test");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.GSM);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            getCardReaderService().checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");
                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
                    getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                            APDUData apduData = createAPDUData(
                                    "0400A404000A4F53452E5641532E303100",
                                    APDUData.ContactInterfaceType.GSM);
                            logReceivedMessage("exchangeAPDU apduData " + apduData);
                            getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    logReceivedMessage("Exchange APDU result : " + s);

                                    if (s.endsWith("6A82")) {
                                        logReceivedMessage("File not found Test Passed");
                                    } else {
                                        logReceivedMessage("APDU response " + s);
                                    }
                                    disconnectFromCard(connectionOptions);
                                }

                                @Override
                                public void onError(PoyntError poyntError) throws RemoteException {
                                    logReceivedMessage("APDU exchange failed " + poyntError);
                                    disconnectFromCard(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card Not Found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoCardRejectionMaster() {
        logReceivedMessage("===============================");
        logReceivedMessage("ISO Payment Card Rejection Test - Master Card");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.GSM);
            logReceivedMessage("connectToCard : connectionOptions " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success : connectionResult " + connectionResult);
                    final APDUData apduData = createAPDUData(
                            "0400A404000E315041592E5359532E444446303100",
                            APDUData.ContactInterfaceType.GSM);
                    logReceivedMessage("exchangeAPDU : apduData " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);
                            if (s.endsWith("9000")) {
                                APDUData apduData2 = createAPDUData(
                                        "0400A4040007A000000004101000",
                                        APDUData.ContactInterfaceType.GSM);
                                logReceivedMessage("exchangeAPDU : apuduData " + apduData2);
                                getCardReaderService().exchangeAPDU(apduData2, new IPoyntExchangeAPDUListener.Stub() {
                                    @Override
                                    public void onSuccess(String s) throws RemoteException {
                                        logReceivedMessage("Exchange APDU result : " + s);
                                        disconnectFromCard(connectionOptions);
                                    }

                                    @Override
                                    public void onError(PoyntError poyntError) throws RemoteException {
                                        logReceivedMessage("APDU exchange failed " + poyntError);
                                        logReceivedMessage("PaymentCard Rejection Success");
                                        disconnectFromCard(connectionOptions);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoExchangeApdu() {
        logReceivedMessage("===============================");
        logReceivedMessage("Exchange ISO APDU Test");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.GSM);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            getCardReaderService().checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");
                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
                    getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success: ConnectionResult " + connectionResult);

                            List<APDUData> apduDataList = new ArrayList<>();
                            apduDataList.add(
                                    createAPDUData(
                                            "0400A404000E315041592E5359532E444446303100",
                                            "6A816A82",
                                            APDUData.ContactInterfaceType.GSM));
                            apduDataList.add(
                                    createAPDUData(
                                            "0400A404000A4F53452E5641532E303100",
                                            "6A816A82",
                                            APDUData.ContactInterfaceType.GSM));
                            apduDataList.add(
                                    createAPDUData(
                                            "0400A404000E315041592E5359532E444446303100",
                                            "6A816A82",
                                            APDUData.ContactInterfaceType.GSM));

                            logReceivedMessage("exchangeAPDUList : apduData " + apduDataList);

                            getCardReaderService().exchangeAPDUList(apduDataList, new IPoyntExchangeAPDUListListener.Stub() {
                                @Override
                                public void onResult(List<String> list, PoyntError poyntError) throws RemoteException {
                                    if (poyntError != null) {
                                        logReceivedMessage("Exchange APDU List failed " + poyntError);
                                    } else {
                                        logReceivedMessage("Exchange APDU result : " + list.toString());
                                        if (list.size() >= 3) {
                                            if (list.get(0).endsWith("9000") || list.get(0).endsWith("6A81") || list.get(0).endsWith("6A82")) {
                                                if (list.get(1).endsWith("9000") || list.get(1).endsWith("6A81") || list.get(1).endsWith("6A82")) {
                                                    if (list.get(2).endsWith("9000") || list.get(2).endsWith("6A81") || list.get(2).endsWith("6A82")) {
                                                        logReceivedMessage("Exchange APDU list test passed");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    disconnectFromCard(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card not found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoPymntTrnDuringDca() {
        logReceivedMessage("===============================");
        logReceivedMessage("ISO Payment transaction during DCA Test");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.GSM);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            getCardReaderService().checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");
                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);
                    getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                        @Override
                        public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                            logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                            APDUData apduData = createAPDUData(
                                    "0400A404000E315041592E5359532E444446303100",
                                    APDUData.ContactInterfaceType.GSM);
                            logReceivedMessage("exchangeAPDU apduData " + apduData);
                            getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    logReceivedMessage("Exchange APDU result : " + s);

                                    if (s.endsWith("6A81") || s.endsWith("6A82") || s.endsWith("9000")) {
                                        logReceivedMessage("Exchange APDU success");
                                        logReceivedMessage("Now open the Terminal app and perform a Payment transaction, Transaction should be success");
                                    } else {
                                        logReceivedMessage("APDU response " + s);
                                    }
                                }

                                @Override
                                public void onError(PoyntError poyntError) throws RemoteException {
                                    logReceivedMessage("APDU exchange failed " + poyntError);
                                    disconnectFromCard(connectionOptions);
                                }
                            });
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("Connection failed : " + poyntError.toString());
                        }
                    });
                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card Not Found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoXAPDU() {
        logReceivedMessage("===============================");
        logReceivedMessage("iso XAPDU");
        try {
            logReceivedMessage("Exchange APDU");
            APDUData apduData = createAPDUData(
                    "0400A404000E315041592E5359532E444446303100",
                    APDUData.ContactInterfaceType.GSM);
            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
            getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    logReceivedMessage("Exchange APDU result : " + s);
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("APDU exchange failed " + poyntError);
                    logReceivedMessage("Test passed");
                    final ConnectionOptions connectionOptions = createConnectionOptions(
                            ConnectionOptions.ContactInterfaceType.GSM);
                    logReceivedMessage("disconnectCardReader : ConnectionOptions : " + connectionOptions);
                    disconnectFromCard(connectionOptions);
                }
            });
        } catch (Exception e) {
            logReceivedMessage(e.getMessage());
        }
    }

    private void isoItalianHealthCards() {
        logReceivedMessage("===============================");
        logReceivedMessage("ISO Italian Health Card Test");

        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.GSM);
            logReceivedMessage("checkIfCardInserted : connectionOptions" + connectionOptions);
            getCardReaderService().checkIfCardInserted(connectionOptions, new IPoyntCardInsertListener.Stub() {
                @Override
                public void onCardFound() throws RemoteException {
                    logReceivedMessage("Card Found");

                    APDUData apduData1 = createAPDUData(
                            "0400A40000023F0000",
                            APDUData.ContactInterfaceType.GSM);

                    APDUData apduData2 = createAPDUData(
                            "0400A4000002110000",
                            APDUData.ContactInterfaceType.GSM);

                    APDUData apduData3 = createAPDUData(
                            "0400A4000002110200",
                            APDUData.ContactInterfaceType.GSM);

                    APDUData apduData4 = createAPDUData(
                            "0300B0000000",
                            APDUData.ContactInterfaceType.GSM);

                    logReceivedMessage("connectToCard : connectionOptions" + connectionOptions);

                    isoConnectToCardObservable(connectionOptions).flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> {
                                return exchangeAPDUListObservable(generateISOApduList(apduData1, "Select MF"), "Select MF", false);
                            }).takeWhile(list -> {
                                if (list.size() > 0) {
                                    logReceivedMessage("APDU exchange Success For :" + apduData1);
                                    return true;
                                } else {
                                    logReceivedMessage("APDU exchange Failed For : " + apduData1);
                                    return false;
                                }
                            }).flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                                    generateISOApduList(apduData2, "Select DF1"), "Select DF1", false))
                            .takeWhile(list -> {
                                if (list.size() > 0) {
                                    logReceivedMessage("APDU exchange Success For : " + apduData2);
                                    return true;
                                } else {
                                    logReceivedMessage("APDU exchange Failed For : " + apduData2);
                                    return false;
                                }
                            })
                            .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                                    generateISOApduList(apduData3, "Select EF"), "Select EF", false))
                            .takeWhile(list -> {
                                if (list.size() > 0) {
                                    logReceivedMessage("APDU exchange Success For : " + apduData3);
                                    return true;
                                } else {
                                    logReceivedMessage("APDU exchange Failed For : " + apduData3);
                                    return false;
                                }
                            })
                            .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                                    generateISOApduList(apduData4, "Read EF"), "Read EF", false))
                            .takeWhile(list -> {
                                if (list.size() > 0) {
                                    logReceivedMessage("APDU exchange Success For : " + apduData4);
                                    return true;
                                } else {
                                    logReceivedMessage("APDU exchange Failed  For : " + apduData4);
                                    return false;
                                }
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(new Observer<List<String>>() {
                                @Override
                                public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                                }

                                @Override
                                public void onNext(@io.reactivex.annotations.NonNull List<String> list) {
                                    logReceivedMessage("Done with the test");
                                    disconnectFromCard(connectionOptions);
                                }

                                @Override
                                public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                    disconnectFromCard(connectionOptions);
                                }

                                @Override
                                public void onComplete() {
                                    disconnectFromCard(connectionOptions);
                                }
                            });

                }

                @Override
                public void onCardNotFound() throws RemoteException {
                    logReceivedMessage("Card not found");
                    logReceivedMessage("Test Failed");
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Error " + poyntError.toString());
                    logReceivedMessage("Test Failed");
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }
    //---------------------------------------------------------------------------------------
    //endregion

    //region SLE 4442 tests
    //---------------------------------------------------------------------------------------
    private void sle401() {
        logReceivedMessage("===============================");
        logReceivedMessage("401 Test for Read Memory");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.SLE);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = createAPDUData(
                            "03A00100000400800008",
                            APDUData.ContactInterfaceType.SLE);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("401 Test for Read Memory  Test Passed");
                            } else {
                                logReceivedMessage("401 Test for Read Memory Test Failed");
                            }
                            disconnectFromCard(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    private void sle402() {
        logReceivedMessage("===============================");
        logReceivedMessage("402 Read with Protect Bit");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.SLE);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = createAPDUData(
                            "03A00400000400100004",
                            APDUData.ContactInterfaceType.SLE);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("402 Read with Protect Bit Test Passed");
                            } else {
                                logReceivedMessage("402 Read with Protect Bit Test Failed");
                            }
                            disconnectFromCard(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    private void sle403() {
        logReceivedMessage("===============================");
        logReceivedMessage("403 Verify Programmable Security Code (PSC)");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.SLE);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = createAPDUData(
                            "03A003000003FFFFFF",
                            APDUData.ContactInterfaceType.SLE);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("403 Verify Programmable Security Code (PSC) Test Passed");
                            } else {
                                logReceivedMessage("403 Verify Programmable Security Code (PSC) Test Failed");
                            }
                            disconnectFromCard(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }


    private void sle404() {
        logReceivedMessage("===============================");
        logReceivedMessage("404 Change Memory");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.SLE);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);

                    List<APDUData> apduDataList = new ArrayList<>();
                    apduDataList.add(
                            createAPDUData(
                                    "03A003000003FFFFFF",
                                    APDUData.ContactInterfaceType.SLE));
                    apduDataList.add(
                            createAPDUData(
                                    "03A00100000400800008",
                                    APDUData.ContactInterfaceType.SLE));
                    apduDataList.add(
                            createAPDUData(
                                    "03A002000003008055",
                                    APDUData.ContactInterfaceType.SLE));
                    apduDataList.add(
                            createAPDUData(
                                    "03A00100000400800008",
                                    APDUData.ContactInterfaceType.SLE));
                    apduDataList.add(
                            createAPDUData(
                                    "03A002000003008022",
                                    APDUData.ContactInterfaceType.SLE));
                    apduDataList.add(
                            createAPDUData(
                                    "03A00100000400800008",
                                    APDUData.ContactInterfaceType.SLE));

                    logReceivedMessage("exchangeAPDUList : apduData " + apduDataList);

                    getCardReaderService().exchangeAPDUList(apduDataList, new IPoyntExchangeAPDUListListener.Stub() {
                        @Override
                        public void onResult(List<String> list, PoyntError poyntError) throws RemoteException {
                            if (poyntError != null) {
                                logReceivedMessage("Exchange APDU List failed " + poyntError);
                            } else {
                                logReceivedMessage("Exchange APDU result : " + list.toString());
                                if (list.size() >= 3) {
                                    if (list.get(0).endsWith("9000") && list.get(1).endsWith("9000") &&
                                            list.get(2).endsWith("9000") && list.get(3).endsWith("9000") &&
                                            list.get(4).endsWith("9000") && list.get(5).endsWith("9000")) {
                                        if (list.get(3).startsWith("55") && list.get(5).startsWith("22")) {
                                            logReceivedMessage("404 Change Memory test passed");
                                        }
                                    }
                                }
                            }
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void sle405() {
        logReceivedMessage("===============================");
        logReceivedMessage("405 Change Memory w/ Protect Bit");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.SLE);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);

                    List<APDUData> apduDataList = new ArrayList<>();
                    apduDataList.add(
                            createAPDUData(
                                    "03A003000003FFFFFF",
                                    APDUData.ContactInterfaceType.SLE));
                    apduDataList.add(
                            createAPDUData(
                                    "03A00400000400100004",
                                    APDUData.ContactInterfaceType.SLE));
                    apduDataList.add(
                            createAPDUData(
                                    "03A0050000030010AB",
                                    APDUData.ContactInterfaceType.SLE));
                    apduDataList.add(
                            createAPDUData(
                                    "03A00400000400100004",
                                    APDUData.ContactInterfaceType.SLE));

                    logReceivedMessage("exchangeAPDUList : apduData " + apduDataList);

                    getCardReaderService().exchangeAPDUList(apduDataList, new IPoyntExchangeAPDUListListener.Stub() {
                        @Override
                        public void onResult(List<String> list, PoyntError poyntError) throws RemoteException {
                            if (poyntError != null) {
                                logReceivedMessage("Exchange APDU List failed " + poyntError);
                            } else {
                                logReceivedMessage("Exchange APDU result : " + list.toString());
                                if (list.size() >= 3) {
                                    if (list.get(0).endsWith("9000") && list.get(1).endsWith("9000") &&
                                            list.get(2).endsWith("9000") && list.get(3).endsWith("9000")) {
                                        if (list.get(3).startsWith("AB")) {
                                            logReceivedMessage("405 Change Memory w/ Protect Bit test passed");
                                        }
                                    }
                                }
                            }
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void sle406() {
        logReceivedMessage("===============================");
        logReceivedMessage("406 Raw Mode ");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.SLE);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = createAPDUData(
                            "03A080000003C1C2C3",
                            APDUData.ContactInterfaceType.SLE);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("406 Raw Mode Test Passed");
                            } else {
                                logReceivedMessage("406 Raw Mode Test Failed");
                            }
                            disconnectFromCard(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void sle407() {
        logReceivedMessage("===============================");
        logReceivedMessage("407 Raw Mode with Response ");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.SLE);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = createAPDUData(
                            "03A0810000053040000008",
                            APDUData.ContactInterfaceType.SLE);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("407 Raw Mode with Response Test passed");
                            } else {
                                logReceivedMessage("407 Raw Mode with Response Test failed");
                            }
                            disconnectFromCard(connectionOptions);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void sle408() {
        logReceivedMessage("===============================");
        logReceivedMessage("408 Payment Transaction during DCA (PoyntC Only)");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.SLE);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = createAPDUData(
                            "03A00100000400800008",
                            APDUData.ContactInterfaceType.SLE);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("Perform a payment transaction");
                            } else {
                                logReceivedMessage("408 Payment Transaction during DCA Test failed");
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void sle409() {
        logReceivedMessage("===============================");
        logReceivedMessage("409 Test Use of the Abort command ");
        try {
            final ConnectionOptions connectionOptions = createConnectionOptions(
                    ConnectionOptions.ContactInterfaceType.SLE);
            logReceivedMessage("connectToCard : ConnectionOptions : " + connectionOptions);
            getCardReaderService().connectToCard(connectionOptions, new IPoyntConnectToCardListener.Stub() {
                @Override
                public void onSuccess(ConnectionResult connectionResult) throws RemoteException {
                    logReceivedMessage("Connection success: ConnectionResult " + connectionResult);
                    APDUData apduData = createAPDUData(
                            "03A00100000400800008",
                            APDUData.ContactInterfaceType.SLE);
                    logReceivedMessage("exchangeAPDU : apduData : " + apduData);
                    getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            logReceivedMessage("Exchange APDU result : " + s);

                            if (s.endsWith("9000")) {
                                logReceivedMessage("Send Abort Command");
                            } else {
                                logReceivedMessage("410 Test Use of the Abort command");
                            }
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("APDU exchange failed " + poyntError);
                            disconnectFromCard(connectionOptions);
                        }
                    });
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("Connection failed : " + poyntError.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logReceivedMessage(e.getMessage());
        }
    }

    private void sleXAPDU() {
        logReceivedMessage("===============================");
        logReceivedMessage("SLE XAPDU");
        try {
            logReceivedMessage("Exchange APDU");
            APDUData apduData = createAPDUData(
                    "03A00100000400800008",
                    APDUData.ContactInterfaceType.SLE);
            logReceivedMessage("exchangeAPDU : apduData : " + apduData);
            getCardReaderService().exchangeAPDU(apduData, new IPoyntExchangeAPDUListener.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    logReceivedMessage("Exchange APDU result : " + s);
                }

                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    logReceivedMessage("APDU exchange failed " + poyntError);
                    logReceivedMessage("Test passed");
                    final ConnectionOptions connectionOptions = createConnectionOptions(
                            ConnectionOptions.ContactInterfaceType.SLE);
                    logReceivedMessage("disconnectFromCard : ConnectionOptions : " + connectionOptions);
                    disconnectFromCard(connectionOptions);
                }
            });
        } catch (Exception e) {
            logReceivedMessage(e.getMessage());
        }
    }
    //---------------------------------------------------------------------------------------
    //endregion

    //misc methods
    //---------------------------------------------------------------------------------------
    protected ArrayList<APDUData> generateISOApduList(APDUData apduData, String logMessage) {
        ArrayList<APDUData> apduList = new ArrayList<>();
        logReceivedMessage(logMessage + " - APDU Exchange - " + apduData);
        apduList.add(apduData);
        return apduList;
    }

    protected Observable<ConnectionResult> isoConnectToCardObservable(ConnectionOptions connectionOptions) {
        return Observable.create(emitter -> {
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
    //endregion
    //---------------------------------------------------------------------------------------
}
