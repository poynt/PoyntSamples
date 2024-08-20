package co.poynt.samples.dcatestapp.ui.fragments;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import co.poynt.os.model.APDUData;
import co.poynt.os.model.ConnectionOptions;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntCardInsertListener;
import co.poynt.os.services.v1.IPoyntConfigurationUpdateListener;
import co.poynt.os.services.v1.IPoyntExchangeAPDUListListener;
import co.poynt.os.services.v1.IPoyntExchangeAPDUListener;
import co.poynt.os.util.ByteUtils;
import co.poynt.samples.dcatestapp.databinding.FragmentMiscTestsBinding;
import co.poynt.samples.dcatestapp.utils.IHelper;

public class MiscTestsFragment extends BaseFragment {
    private FragmentMiscTestsBinding binding;

    public MiscTestsFragment(IHelper helper) {
        super(helper);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMiscTestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //misc tests
        binding.connectToCard.setOnClickListener(v -> connectToCard());
        binding.disconnectFromCard.setOnClickListener(v -> disconnectFromCard());
        binding.checkIfCardInserted.setOnClickListener(v -> checkIfCardInserted());
        binding.abort.setOnClickListener(v -> abortCardRead());
        binding.readIMSI.setOnClickListener(v -> readIMSI());

        binding.exchangeAPDU.setOnClickListener(v -> exchangeAPDU());
        binding.exchangeAPDUList.setOnClickListener(v -> exchangeAPDUList());
        binding.btnSendBinRange.setOnClickListener(v -> setBinRange());
    }

    //region utility methods
    // ---------------------------------------------------------------------------------------
    private void selectMasterfile(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = createAPDUData(
                "04A0A40000023F0000",
                APDUData.ContactInterfaceType.GSM);
        try {
            getCardReaderService().exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void getSelectMasterfileResponse(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = createAPDUData(
                "02A0C0000000",
                APDUData.ContactInterfaceType.GSM);
        try {
            getCardReaderService().exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void selectGSMDirectory(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = createAPDUData(
                "04A0A40000027F2000",
                APDUData.ContactInterfaceType.GSM);
        try {
            getCardReaderService().exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void selectIMSI(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = createAPDUData(
                "04A0A40000026F0700",
                APDUData.ContactInterfaceType.GSM);
        try {
            getCardReaderService().exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void getSelectIMSIResponse(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = createAPDUData(
                "02A0C0000000",
                APDUData.ContactInterfaceType.GSM);
        try {
            getCardReaderService().exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void readBinaryIMSI(IPoyntExchangeAPDUListener listener) {
        APDUData apduData = createAPDUData(
                "02A0B0000009",
                APDUData.ContactInterfaceType.GSM);
        try {
            getCardReaderService().exchangeAPDU(apduData, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    //endregion
    // ---------------------------------------------------------------------------------------


    //region Misc Tests
    // ---------------------------------------------------------------------------------------
    private void checkIfCardInserted() {
        ConnectionOptions connectionOptions = createConnectionOptions(
                ConnectionOptions.ContactInterfaceType.GSM,
                60);
        try {
            getCardReaderService().checkIfCardInserted(connectionOptions,
                    new IPoyntCardInsertListener.Stub() {

                        @Override
                        public void onCardFound() throws RemoteException {
                            logReceivedMessage("Card Found");
                        }

                        @Override
                        public void onCardNotFound() throws RemoteException {
                            logReceivedMessage("Card Not Found");
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("onError: " + poyntError.toString());
                        }
                    });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void abortCardRead() {
        try {
            logReceivedMessage("Aborting....");
            getCardReaderService().abort();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void readIMSI() {
        selectMasterfile(
                new IPoyntExchangeAPDUListener.Stub() {
                    @Override
                    public void onSuccess(String rAPDU) throws RemoteException {
                        getSelectMasterfileResponse(new Stub() {
                            @Override
                            public void onSuccess(String rAPDU) throws RemoteException {
                                logReceivedMessage("Response of Select Masterfile: " + rAPDU);
                                selectGSMDirectory(new Stub() {
                                    @Override
                                    public void onSuccess(String rAPDU) throws RemoteException {
                                        logReceivedMessage("Response of Select GSM Directory: " + rAPDU);
                                        selectIMSI(new Stub() {
                                            @Override
                                            public void onSuccess(String rAPDU) throws RemoteException {
                                                logReceivedMessage("Response of select IMSI: " + rAPDU);
                                                getSelectIMSIResponse(new Stub() {
                                                    @Override
                                                    public void onSuccess(String rAPDU) throws RemoteException {
                                                        logReceivedMessage("Response of read IMSI: " + rAPDU);
                                                        readBinaryIMSI(new Stub() {
                                                            @Override
                                                            public void onSuccess(String rAPDU) throws RemoteException {
                                                                logReceivedMessage("Response of read IMSI: " + rAPDU);
                                                            }

                                                            @Override
                                                            public void onError(PoyntError poyntError) throws RemoteException {
                                                                logReceivedMessage("Read IMSI failed: " + poyntError.toString());
                                                            }
                                                        });
                                                    }

                                                    @Override
                                                    public void onError(PoyntError poyntError) throws RemoteException {
                                                        logReceivedMessage("Read IMSI failed: " + poyntError.toString());
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onError(PoyntError poyntError) throws RemoteException {
                                                logReceivedMessage("Select IMSI failed: " + poyntError.toString());
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(PoyntError poyntError) throws RemoteException {
                                        logReceivedMessage("Select GSM Directory failed: " + poyntError.toString());
                                    }
                                });
                            }

                            @Override
                            public void onError(PoyntError poyntError) throws RemoteException {
                                logReceivedMessage("Read masterfile select failed: " + poyntError.toString());
                            }
                        });

                    }

                    @Override
                    public void onError(PoyntError poyntError) throws RemoteException {
                        logReceivedMessage("Select Masterfile failed: " + poyntError.toString());
                    }
                });
    }

    private void exchangeAPDU() {
        APDUData apduData = createAPDUData(
                binding.apduDataInput.getText().toString(),
                APDUData.ContactInterfaceType.GSM);
        try {
            getCardReaderService().exchangeAPDU(apduData,
                    new IPoyntExchangeAPDUListener.Stub() {
                        @Override
                        public void onSuccess(String rAPDU) throws RemoteException {
                            logReceivedMessage("Response APDU: " + rAPDU);
                        }

                        @Override
                        public void onError(PoyntError poyntError) throws RemoteException {
                            logReceivedMessage("onError: " + poyntError.toString());
                        }
                    });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void exchangeAPDUList() {
        List<APDUData> apduDataList = new ArrayList<>();
        // Split the line by ';'
        String[] parts = binding.apduListDataInput.getText().toString().split(";");
        for (String part : parts) {
            // Split each part by ',' if available
            String[] subParts = part.split(",");

            APDUData apduData = createAPDUData(
                    subParts[0],
                    subParts.length == 2 ? subParts[1] : null,
                    APDUData.ContactInterfaceType.GSM,
                    60);

            apduDataList.add(apduData);
        }

        try {
            getCardReaderService().exchangeAPDUList(apduDataList,
                    new IPoyntExchangeAPDUListListener.Stub() {
                        @Override
                        public void onResult(List<String> responseAPDUData, PoyntError poyntError) throws RemoteException {
                            if (poyntError != null) {
                                logReceivedMessage("Exchange APDU List failed " + poyntError);
                            } else {
                                logReceivedMessage("Exchange APDU result : " + responseAPDUData.toString());
                            }
                        }
                    });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void setBinRange() {
        // send setTerminalConfiguration
        byte mode = (byte) 0x00;
        // bin ranges only apply to MSR
        byte cardInterface = (byte) 0x01;
        // bin range
        String binRangeTag = "1F812F" + "0706" + binding.etBinRange.getText().toString();
        ByteArrayOutputStream ptOs = null;
        try {
            ptOs = new ByteArrayOutputStream();
            ptOs.write(ByteUtils.hexStringToByteArray(binRangeTag));
            getPoyntConfigurationService().setTerminalConfiguration(mode, cardInterface,
                    ptOs.toByteArray(),
                    new IPoyntConfigurationUpdateListener.Stub() {
                        @Override
                        public void onSuccess() throws RemoteException {
                            logReceivedMessage("Bin Range Successfully updated");
                        }

                        @Override
                        public void onFailure() throws RemoteException {
                            logReceivedMessage("Bin Range updated failed!");
                        }
                    });
        } catch (Exception e) {
            logReceivedMessage("Failed to setTerminalConfiguration");
        }
    }
    //---------------------------------------------------------------------------------------
    //endregion
}
