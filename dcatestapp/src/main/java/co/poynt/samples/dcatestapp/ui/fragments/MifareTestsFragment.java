package co.poynt.samples.dcatestapp.ui.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import co.poynt.os.model.APDUData;
import co.poynt.os.model.ConnectionResult;
import co.poynt.os.util.ByteUtils;
import co.poynt.samples.dcatestapp.databinding.FragmentMifareTestsBinding;
import co.poynt.samples.dcatestapp.utils.IHelper;
import co.poynt.samples.dcatestapp.utils.Utils;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MifareTestsFragment extends BaseFragment {
    private FragmentMifareTestsBinding binding;

    public MifareTestsFragment(IHelper helper) {
        super(helper);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMifareTestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Mifare classic tests
        binding.testMifare.setOnClickListener(v -> startMifareTest());
        binding.testMifareAfterPowerCycle.setOnClickListener(v -> startMifareTestAfterPowerCycle());

        //Mifare Desfire tests
        binding.desfire601.setOnClickListener(v -> desfire601());
        binding.desfire602.setOnClickListener(v -> desfire602());
        binding.desfire603.setOnClickListener(v -> desfire603());
        binding.desfire604.setOnClickListener(v -> desfire604());
        binding.desfire605.setOnClickListener(v -> desfire605());
        binding.desfire606.setOnClickListener(v -> desfire606());
        binding.desfire607.setOnClickListener(v -> desfire607());
        binding.desfire610.setOnClickListener(v -> desfire610());
        binding.desfire611.setOnClickListener(v -> desfire611());
        binding.desfire612.setOnClickListener(v -> desfire612());
        binding.desfire613.setOnClickListener(v -> desfire613());
        binding.desfire614.setOnClickListener(v -> desfire614());
        binding.desfire615.setOnClickListener(v -> desfire615());
        binding.desfire616.setOnClickListener(v -> desfire616());
        binding.desfire617.setOnClickListener(v -> desfire617());

        //Mifare Ultralight tests
        binding.ultralight801.setOnClickListener(v -> ultralight801());
        binding.ultralight802.setOnClickListener(v -> ultralight802());
        binding.ultralight803.setOnClickListener(v -> ultralight803());
        binding.ultralight804.setOnClickListener(v -> ultralight804());
        binding.ultralight805.setOnClickListener(v -> ultralight805());
        binding.ultralight806.setOnClickListener(v -> ultralight806());
        binding.ultralight807.setOnClickListener(v -> ultralight807());
        binding.ultralight808.setOnClickListener(v -> ultralight808());
        binding.ultralight809.setOnClickListener(v -> ultralight809());
        binding.ultralight810.setOnClickListener(v -> ultralight810());
        binding.ultralight811.setOnClickListener(v -> ultralight811());
        binding.ultralight812.setOnClickListener(v -> ultralight812());
        binding.ultralight813.setOnClickListener(v -> ultralight813());
        binding.ultralight814.setOnClickListener(v -> ultralight814());
        binding.ultralight815.setOnClickListener(v -> ultralight815());
        binding.ultralight816.setOnClickListener(v -> ultralight816());
        binding.ultralight817.setOnClickListener(v -> ultralight817());
        binding.ultralight818.setOnClickListener(v -> ultralight818());
        binding.ultralight819.setOnClickListener(v -> ultralight819());
        binding.ultralight820.setOnClickListener(v -> ultralight820());
        binding.ultralight830.setOnClickListener(v -> ultralight830());
    }

    //region TEST DCA MIFARE tests
    //---------------------------------------------------------------------------------------
    @SuppressLint("CheckResult")
    private void startMifareTest() {
        logReceivedMessage("=========== TEST DCA MIFARE ==================");
        logReceivedMessage("Start Mifare test");
        logReceivedMessage("Please, tap the card");

        String key = "FFFFFFFFFFFF";

        final ConnectionResult.CardType[] cardType = {ConnectionResult.CardType.MIFARE_CLASSIC_1K};
        final String[] test3ResponseInvertedData = new String[1];
        final String[] test6ResponseInvertedData = new String[1];

        connectToCardObservable()
                .flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> {
                    cardType[0] = (connectionResult.getCardType());
                    return exchangeAPDUListObservable(generateApduList(2,
                                    "Authenticate Block 6 KeyA:", ("03A0100000080601" + key)),
                            "Test ability to authenticate Sector 1 in the Mifare Classic card",
                            false);
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(3, "Read Block 6, 1 block: ",
                                "03A0110000020601"),
                        "Test the ability to read to an authenticated sector",
                        false))
                .takeWhile(list -> {
                    if (list != null && list.size() > 0 && list.get(0).length() >= 16) {
                        logReceivedMessage("Device returns 16 bytes of data: Success");
                        return true;
                    } else {
                        logReceivedMessage("Device returns less than 16 bytes of data: Failed");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> {
                    byte[] responseData = ByteUtils.hexStringToByteArray(list.get(0));
                    byte[] invertedResponseData = Utils.invertBytes(responseData, 0, responseData.length);
                    test3ResponseInvertedData[0] = ByteUtils.byteArrayToHexString(invertedResponseData);

                    return exchangeAPDUListObservable(
                            generateApduList(4,
                                    "Write Block 6 previous data inverted",
                                    ("03A01200001106" + test3ResponseInvertedData[0])),
                            "Test the ability to write to an authenticated sector",
                            false);
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(5,
                                "Read Block 6, 1 block: ",
                                "03A0110000020601"),
                        "Test to validate previous write was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data that matches the inverted data written in previous test
                    if (list.get(0).equals(test3ResponseInvertedData[0])) {
                        logReceivedMessage("The data matches the inverted data written in previous test, PASSED");
                        return true;
                    } else {
                        logReceivedMessage("The data don't match the inverted data written in previous test, FAILED");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(6,
                                "Read Block 14",
                                "03A0110000020E01"),
                        "Verify that automatic authentication occurs on a read to a block in a different sector",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> {
                    byte[] responseData = ByteUtils.hexStringToByteArray(list.get(0));
                    byte[] invertedResponseData = Utils.invertBytes(responseData, 0, responseData.length);
                    test6ResponseInvertedData[0] = ByteUtils.byteArrayToHexString(invertedResponseData);
                    return exchangeAPDUListObservable(
                            generateApduList(7,
                                    "Write Block 14 previous data inverted",
                                    ("03A0120000110E" + test6ResponseInvertedData[0])),
                            "Verify the ability to write to an automatically authenticated block",
                            false);
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(8,
                                "Read Block 14",
                                "03A0110000020E01"),
                        "Test to validate previous write was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data that matches the inverted data written in previous test
                    if (list.get(0).equals(test6ResponseInvertedData[0])) {
                        logReceivedMessage("The data matches the inverted data written in previous test, PASSED");
                        return true;
                    } else {
                        logReceivedMessage("The data don't match the inverted data written in previous test, FAILED");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(9,
                                "Write Block 4 with 00’s",
                                "03A0120000110400000000FFFFFFFF0000000004FB04FB"),
                        "Clear the block to prepare for value tests",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(10,
                                "Read Block 4 ",
                                "03A0110000020401"),
                        "Test to validate previous write was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data all zeroes
                    if (list != null && list.size() > 0 && list.get(0).contains("00000000FFFFFFFF00000000")) {
                        logReceivedMessage("Device returns data 05000000: Passed");
                        return true;
                    } else {
                        logReceivedMessage("Device returns data 05000000: Failed");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(11,
                                "Increment Block 4 by 10 ",
                                "03A01300000604040A000000"),
                        "Verify block may be incremented",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(12,
                                "Read Block 4",
                                "03A0110000020401"),
                        "Test to validate previous increment was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data 0A000000
                    if (list != null && list.size() > 0 && list.get(0).contains("0A000000F5FFFFFF0A000000")) {
                        logReceivedMessage("Device returns data 0A000000: Passed");
                        return true;
                    } else {
                        logReceivedMessage("Device returns data 0A000000: Failed");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(13,
                                "Decrement Block 4 by 5",
                                "03A014000006040405000000"),
                        "Verify block may be decremented",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(14,
                                "Read Block 4",
                                "03A0110000020401"),
                        "Test to validate previous decrement was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data 05000000
                    if (list != null && list.size() > 0 && list.get(0).contains("05000000FAFFFFFF05000000")) {
                        logReceivedMessage("Device returns data 05000000: Passed");
                        return true;
                    } else {
                        logReceivedMessage("Device returns data 05000000: Failed");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(15,
                                "Move Block 4 to Block 5",
                                "03A0150000020405"),
                        "Verify use of the Move command",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(16, "Read Block 5",
                                "03A0110000020501"),
                        "Verify previous move was successful",
                        false))
                .takeWhile(list -> {
                    //Device returns data 05000000
                    if (list != null && list.size() > 0 && list.get(0).contains("05000000FAFFFFFF05000000")) {
                        logReceivedMessage("Device returns data 05000000: Passed");
                        return true;
                    } else {
                        logReceivedMessage("Device returns data 05000000: Failed");
                        return false;
                    }
                })
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> {
                    if (cardType[0] == ConnectionResult.CardType.MIFARE_CLASSIC_1K) {
                        return exchangeAPDUListObservable(
                                generateApduList(17,
                                        "1K Card - Read Block 64", "03A0110000024001"),
                                "If 1K card, test for out of range",
                                true);
                    } else {
                        return exchangeAPDUListObservable(
                                generateApduList(18,
                                        "4K Card - Read Block 64 and 255",
                                        "03A0110000024001", "03A011000002FF01"),
                                "If 4K card test range over 1K",
                                false);
                    }
                })
                .flatMap((Function<List<String>, Observable<String>>) list -> {
                    logReceivedMessage("Wait 10 sec for card disconnect");
                    return disconnectFromCardObservable();
                })
                .delay(10, TimeUnit.SECONDS)
                .flatMap((Function<String, Observable<ConnectionResult>>) disconnectResult -> connectToCardObservable())
                .takeWhile(connectionResult -> {
                    if (connectionResult.getCardType() == cardType[0]) {
                        logReceivedMessage("Device returns correct Mifare Classic card: Success");
                        return true;
                    } else {
                        logReceivedMessage("Device returns incorrect Mifare Classic card: Failed");
                        return false;
                    }
                })
                .flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> exchangeAPDUListObservable(
                        generateApduList(21,
                                "Read Block 6, 1 block: ",
                                "03A0110000020601"),
                        "Test that keys have been erased",
                        true))
                .flatMap((Function<List<String>, Observable<String>>) list -> {
                    logReceivedMessage("Wait 10 sec for card disconnect");
                    return disconnectFromCardObservable();
                })
                .delay(10, TimeUnit.SECONDS)
                .flatMap((Function<String, Observable<ConnectionResult>>) disconnectResult -> connectToCardObservable())
                .takeWhile(connectionResult -> {
                    if (connectionResult.getCardType() == cardType[0]) {
                        logReceivedMessage("Device returns correct Mifare Classic card: Success");
                        return true;
                    } else {
                        logReceivedMessage("Device returns incorrect Mifare Classic card: Failed");
                        return false;
                    }
                })
                .flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> exchangeAPDUListObservable(
                        generateApduList(24,
                                "Authenticate Block 6 KeyA:", ("03A0100000080601" + key)),
                        "Test ability to authenticate Sector 1",
                        false))
                .flatMap((Function<List<String>, Observable<List<String>>>) list -> exchangeAPDUListObservable(
                        generateApduList(25,
                                "Read Block 6, 1 block: ",
                                "03A0110000020601"),
                        "Test the ability to read to an authenticated sector",
                        false))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<List<String>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull List<String> list) {
                        logReceivedMessage("Please, reboot the terminal. After reboot completed," +
                                " start second test button (Test mifare (after power cycle))");
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        disconnectFromCard();
                    }

                    @Override
                    public void onComplete() {
                        disconnectFromCard();
                    }
                });
    }

    private void startMifareTestAfterPowerCycle() {
        connectToCardObservable()
                .flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> exchangeAPDUListObservable(
                        generateApduList(27,
                                "Read Block 6, 1 block: ",
                                "03A0110000020601"),
                        "Test ability to erase keys on power cycle",
                        true))
                .flatMap((Function<List<String>, Observable<String>>) list -> {
                    logReceivedMessage("Wait 10 sec for card disconnect");
                    return disconnectFromCardObservable();
                })
                .delay(10, TimeUnit.SECONDS)
                .flatMap((Function<String, Observable<ConnectionResult>>) disconnectResult -> connectToCardObservable())
                .flatMap((Function<ConnectionResult, Observable<List<String>>>) connectionResult -> exchangeAPDUListObservable(
                        generateApduList(31,
                                "Authenticate Block 6 KeyA:",
                                "03A0100000080601112233445566"),
                        "Test for attempt to authenticate with an invalid key ",
                        true))
                .flatMap((Function<List<String>, Observable<String>>) list -> disconnectFromCardObservable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull String s) {

                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        disconnectFromCard();
                    }
                });
    }
    //---------------------------------------------------------------------------------------
    //endregion

    //region Mifare Desfire tests
    //---------------------------------------------------------------------------------------
    private void desfire601() {
        String testName = "601 DESFire Card Detected ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        Disposable disposable = connectToCardObservable()
                .flatMap(connectionResult -> {
                    if ((connectionResult.getCardType() == ConnectionResult.CardType.MIFARE_DESFIRE
                            || connectionResult.getCardType() == ConnectionResult.CardType.MIFARE_DESFIRE_LIGHT)
                            && connectionResult.getAtsData() != null
                            && connectionResult.getSakData() != null) {
                        return Observable.just(connectionResult);
                    } else {
                        return Observable.error(new Exception("Incorrect card detected"));
                    }
                })
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(connectionResult -> {
                    logReceivedMessage(testName + " Passed");
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire602() {
        String testName = "602: L4 Get Version ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        List<APDUData> apduDataList = new ArrayList<>();
        apduDataList.add(createAPDUData("039060000000", "91AF"));
        apduDataList.add(createAPDUData("0390AF000000", "91AF"));
        apduDataList.add(createAPDUData("0390AF000000", "9100"));

        Disposable disposable = exchangeAPDUListObservable(
                apduDataList,
                testName,
                false,
                true)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(list -> {
                    boolean testPassed = list != null
                            && list.size() == 3
                            && list.get(0).endsWith("91AF")
                            && list.get(1).endsWith("91AF")
                            && list.get(2).endsWith("9100");
                    logReceivedMessage(testName + " " + (testPassed ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire603() {
        String testName = "603: ISO Select File ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("0300A4040C10A00000039656434103F015400000000B00");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean testPassed = result.endsWith("9000");
                    logReceivedMessage(testName + (testPassed ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire604() {
        String testName = "604: Get File ID’s ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("03906F000000");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.equals("0F1F030001049100");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire605() {
        String testName = "605: Get File Settings ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("0390F50000011F00");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.equals("000030EF2000009100");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire606() {
        String testName = "606: Read Data ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("0390AD0000071F00000020000000");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.equals("00000000000000000000000000000000000000000000000000000000000000009100");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire607() {
        String testName = "607: Read Credit Value ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("03906C0000010300");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.equals("000000009100");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire610() {
        String testName = "610: Authenticate ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        String defaultKey = "00000000000000000000000000000000";
        String randomNumber = "00112233445566778899AABBCCDDEEFF";

        Disposable disposable = authenticateObservable()
                .flatMap(response -> {
                    String responseWithoutSW1SW2 = response.substring(0, response.length() - 4);
                    String decryptedResponse = Utils.decryptDataAES(responseWithoutSW1SW2, defaultKey);
                    logReceivedMessage("decrypted response: " + decryptedResponse);
                    String rotatedString = Utils.rotateStringByOneByte(decryptedResponse);
                    logReceivedMessage("rotated response: " + rotatedString);
                    String dataToEncrypt = randomNumber + rotatedString;
                    logReceivedMessage("data to encrypt: " + dataToEncrypt);
                    String encryptedRequest = Utils.encryptDataAES(dataToEncrypt, defaultKey);
                    logReceivedMessage("encrypted request: " + encryptedRequest);
                    return Observable.just(encryptedRequest);
                }).flatMap(encryptedRequest -> {
                    String encryptCommand = "0390AF000020" + encryptedRequest + "00";
                    APDUData apduData = createAPDUData(encryptCommand, "9100");
                    return exchangeAPDUObservable(apduData);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    String responseWithoutSW1SW2 = result.substring(0, result.length() - 4);
                    String decryptedResponse = Utils.decryptDataAES(responseWithoutSW1SW2, defaultKey);
                    logReceivedMessage("decrypted response: " + decryptedResponse);
                    String randomNumberFromResponse = decryptedResponse.substring(8, 40);
                    logReceivedMessage("random number from response: " + randomNumberFromResponse);
                    String rotatedRandomNumber = Utils.rotateStringByOneByte(randomNumber);
                    logReceivedMessage("rotated random number: " + rotatedRandomNumber);
                    boolean success = result.endsWith("9100") && rotatedRandomNumber.equals(randomNumberFromResponse);
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire611() {
        String testName = "611: Authenticate failure ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        Disposable disposable = authenticateObservable()
                .flatMap(response -> {
                    APDUData invalidEncryptionData = createAPDUData(
                            "0390AF000020C8A331FF8EDD3DB175E1545DBEFB760BAAF2127225A49244083F9210D1C9E57900");
                    return exchangeAPDUObservable(invalidEncryptionData);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.equals("91AE");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire612() {
        String testName = "612:  Write Data ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        Disposable disposable = authenticateObservable()
                .flatMap(response -> {
                    APDUData writeData = createAPDUData("03908D0000001F010000000000");
                    return exchangeAPDUObservable(writeData);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.equals("000000009100");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> {
                    logReceivedMessage(testName + "Failed");
                    throwable.printStackTrace();
                    disconnectFromCard();
                });
        compositeDisposable.add(disposable);
    }

    private void desfire613() {
        String testName = "613: Increment Credit Value ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        Disposable disposable = authenticateObservable()
                .flatMap(response -> {
                    APDUData creditCommand = createAPDUData("03900C0000051F010000000000");
                    return exchangeAPDUObservable(creditCommand);
                })
                .flatMap(response -> {
                    //check previous command was correct
                    if (response.endsWith("9100")) {
                        APDUData commitTransaction = createAPDUData("03900000010000");
                        return exchangeAPDUObservable(commitTransaction);
                    } else {
                        return Observable.just(response);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.equals("000000009100");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire614() {
        String testName = "614: Decrement Credit Value ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        Disposable disposable = authenticateObservable()
                .flatMap(response -> {
                    APDUData creditCommand = createAPDUData("0390DC0000051F010000000000");
                    return exchangeAPDUObservable(creditCommand);
                })
                .flatMap(response -> {
                    //check previous command was correct
                    if (response.endsWith("9100")) {
                        APDUData commitTransaction = createAPDUData("03900000010000");
                        return exchangeAPDUObservable(commitTransaction);
                    } else {
                        return Observable.just(response);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.equals("000000009100");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire615() {
        String testName = "615: Test Multiple APDUs, No OK List ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        List<APDUData> apduDataList = new ArrayList<>();
        apduDataList.add(createAPDUData("0300A4040C10A00000039656434103F015400000000B00"));
        apduDataList.add(createAPDUData("0300A4040C10A00000039656434103F015400000000B00"));
        apduDataList.add(createAPDUData("0300A4040C10A00000039656434103F015400000000B00"));

        Disposable disposable = exchangeAPDUListObservable(
                apduDataList,
                testName,
                false,
                true)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(list -> {
                    boolean success = list != null
                            && list.size() == 3
                            && list.get(0).endsWith("9000")
                            && list.get(1).endsWith("9000")
                            && list.get(2).endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);

    }

    private void desfire616() {
        String testName = "616: Test Multiple APDUs with an OK List ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        List<APDUData> apduDataList = new ArrayList<>();
        apduDataList.add(createAPDUData("039060000000", "91AF9100"));
        apduDataList.add(createAPDUData("0390AF000000", "91AF9100"));
        apduDataList.add(createAPDUData("0390AF000000", "91009000"));

        Disposable disposable = exchangeAPDUListObservable(
                apduDataList,
                testName,
                false,
                true)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(list -> {
                    boolean success = list != null
                            && list.size() == 3
                            && list.get(0).endsWith("91AF")
                            && list.get(1).endsWith("91AF")
                            && list.get(2).endsWith("9100");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void desfire617() {
        String testName = "617: Test Multiple APDUs with failure on OK List ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        List<APDUData> apduDataList = new ArrayList<>();
        apduDataList.add(createAPDUData("03906000000", "91AF9100"));
        apduDataList.add(createAPDUData("0390AF000000", "9100"));
        apduDataList.add(createAPDUData("0390AF000000", "9100"));

        Disposable disposable = exchangeAPDUListObservable(
                apduDataList,
                testName,
                false,
                true)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .doFinally(this::disconnectFromCard)
                .subscribe(list -> {
                    boolean success = list != null
                            && list.size() == 2
                            && list.get(0).endsWith("91AF")
                            && list.get(1).endsWith("91AF");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> {
                    logReceivedMessage(testName + "Failed");
                    throwable.printStackTrace();
                });
        compositeDisposable.add(disposable);
    }
    //---------------------------------------------------------------------------------------
    //endregion

    //region Mifare Ultralight tests
    //---------------------------------------------------------------------------------------
    private void ultralight801() {
        String testName = "801 Ultralight Card Detected ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        Disposable disposable = connectToCardObservable()
                .flatMap(connectionResult -> {
                    if (connectionResult.getCardType() == ConnectionResult.CardType.MIFARE_ULTRALITE
//                            && connectionResult.getAtsData() != null // not sure if null is correct or not
                            && connectionResult.getSakData() != null) {
                        return Observable.just(connectionResult);
                    } else {
                        return Observable.error(new Exception("Incorrect card detected"));
                    }
                })
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(connectionResult -> {
                    logReceivedMessage(testName + " Passed");
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight802() {
        String testName = "802: L3 Get Version ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("03A016000000");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight803() {
        String testName = "803: Read ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("03A01700000104");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight804() {
        String testName = "804: Fast Read ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("03A0180000020404");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight805() {
        String testName = "805: Write ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        String[] first4Bytes = new String[1];
        String[] invertedFirst4Bytes = new String[1];

        APDUData readData = createAPDUData("03A0180000020404");

        //fast read data
        Disposable disposable = exchangeAPDUObservable(readData)
                .flatMap(response -> {
                    //write inverted bytes
                    first4Bytes[0] = response.substring(0, 8);
                    invertedFirst4Bytes[0] = Utils.rotateBytes(first4Bytes[0]);
                    APDUData writeData = createAPDUData("03A01900000504" + invertedFirst4Bytes[0]);
                    return exchangeAPDUObservable(writeData);
                })
                .flatMap(response -> {
                    //fast read data with inverted bytes
                    return exchangeAPDUObservable(readData).flatMap(response2 -> {
                        if (!response2.startsWith(invertedFirst4Bytes[0])) {
                            logReceivedMessage(testName + " inverted data verification Failed");
                        }
                        return Observable.just(response2);
                    });
                })
                .flatMap(response -> {
                    //write initial data
                    APDUData writeData = createAPDUData("03A01900000504" + first4Bytes[0]);
                    return exchangeAPDUObservable(writeData);
                })
                .flatMap(response -> {
                    //fast read initial data
                    return exchangeAPDUObservable(readData).flatMap(response2 -> {
                        if (!response2.startsWith(first4Bytes[0])) {
                            logReceivedMessage(testName + " original data verification Failed");
                        }
                        return Observable.just(response2);
                    });
                })
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.startsWith(first4Bytes[0]);
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight806() {
        String testName = "806: Read Counter ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("03A01A00000101");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight807() {
        String testName = "807: Increment Counter ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("03A01B0000050101000000");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight808() {
        String testName = "808: Password Authentication ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        showInputDialog(testName, new ITextInputCallback() {
            @Override
            public void onTextEntered(String text) {
                APDUData apduData = createAPDUData("03A01C000004" + text);

                Disposable disposable = exchangeAPDUObservable(apduData)
                        .firstElement()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(result -> {
                            boolean success = result.endsWith("112233449000");
                            logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                        }, throwable -> disconnectOnException(testName, throwable));
                compositeDisposable.add(disposable);
            }

            @Override
            public void onCancel() {
                logReceivedMessage(testName + " cancelled");
            }
        });
    }

    private void ultralight809() {
        String testName = "809: Read Signature EV1 ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("03A01D000000");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight810() {
        String testName = "810: Read Signature AES ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("03A01D000000");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight811() {
        String testName = "811: Check Tearing Event ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("03A01E00000101");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight812() {
        String testName = "812: VCSL ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData apduData = createAPDUData("03A01F0000141111111111111111111111111111111122222222");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight813() {
        String testName = "813: Authenticate C ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        String defaultKey = "49454D4B41455242214E4143554F5946";
        String randomNumber = "0011223344556677";//8899AABBCCDDEEFF";

        APDUData authenticate = createAPDUData("03A02000000100", "9000");
        Disposable disposable = exchangeAPDUObservable(authenticate)
                .flatMap(response -> {
                    String icv = "0000000000000000";
                    logReceivedMessage("icv1: " + icv);
                    String responseWithoutSW1SW2 = response.substring(0, response.length() - 4);
                    String decryptedResponse = Utils.decryptData3DES(responseWithoutSW1SW2, defaultKey,icv);
                    logReceivedMessage("decrypted response: " + decryptedResponse);
                    String rotatedString = Utils.rotateStringByOneByte(decryptedResponse);
                    logReceivedMessage("rotated response: " + rotatedString);
                    String dataToEncrypt = randomNumber + rotatedString;
                    logReceivedMessage("data to encrypt: " + dataToEncrypt);
                    icv = responseWithoutSW1SW2;
                    logReceivedMessage("icv2: " + icv);
                    String encryptedRequest = Utils.encryptData3DES(dataToEncrypt, defaultKey, icv);
                    logReceivedMessage("encrypted request: " + encryptedRequest);
                    return Observable.just(encryptedRequest);
                }).flatMap(encryptedRequest -> {
                    String encryptCommand = "03A021000010" + encryptedRequest;
                    APDUData apduData = createAPDUData(encryptCommand, "9000");
                    return exchangeAPDUObservable(apduData).flatMap(response -> {
                        String icv = encryptedRequest.substring(16);
                        logReceivedMessage("icv3: " + icv);
                        logReceivedMessage("encrypted request: " + encryptedRequest);
                        String responseWithoutSW1SW2 = response.substring(0, response.length() - 4);
                        String decryptedResponse = Utils.decryptData3DES(responseWithoutSW1SW2, defaultKey, icv);
                        logReceivedMessage("decrypted response: " + decryptedResponse);
                        String rotatedRandomNumber = Utils.rotateStringByOneByte(randomNumber);
                        logReceivedMessage("rotated random number: " + rotatedRandomNumber);
                        boolean success = response.endsWith("9000") && rotatedRandomNumber.equals(decryptedResponse);
                        return Observable.just(success);
                    });
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        result -> logReceivedMessage(testName + (result ? "Passed" : "Failed")),
                        throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight814() {
        String testName = "814: Authenticate AES ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        String defaultKey = "00000000000000000000000000000000";
        String randomNumber = "00112233445566778899AABBCCDDEEFF";

        APDUData authenticate = createAPDUData("03A02000000100", "9000");
        Disposable disposable = exchangeAPDUObservable(authenticate)
                .flatMap(response -> {
                    String responseWithoutSW1SW2 = response.substring(0, response.length() - 4);
                    String decryptedResponse = Utils.decryptDataAES(responseWithoutSW1SW2, defaultKey);
                    logReceivedMessage("decrypted response: " + decryptedResponse);
                    String rotatedString = Utils.rotateStringByOneByte(decryptedResponse);
                    logReceivedMessage("rotated response: " + rotatedString);
                    String dataToEncrypt = randomNumber + rotatedString;
                    logReceivedMessage("data to encrypt: " + dataToEncrypt);
                    String encryptedRequest = Utils.encryptDataAES(dataToEncrypt, defaultKey);
                    logReceivedMessage("encrypted request: " + encryptedRequest);
                    return Observable.just(encryptedRequest);
                }).flatMap(encryptedRequest -> {
                    String encryptCommand = "03A021000020" + encryptedRequest;
                    APDUData apduData = createAPDUData(encryptCommand, "9000");
                    return exchangeAPDUObservable(apduData);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    String responseWithoutSW1SW2 = result.substring(0, result.length() - 4);
                    String decryptedResponse = Utils.decryptDataAES(responseWithoutSW1SW2, defaultKey);
                    logReceivedMessage("decrypted response: " + decryptedResponse);
                    String rotatedRandomNumber = Utils.rotateStringByOneByte(randomNumber);
                    logReceivedMessage("rotated random number: " + rotatedRandomNumber);
                    boolean success = result.endsWith("9000") && rotatedRandomNumber.equals(decryptedResponse);
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight815() {
        String testName = "815: Write Signature ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        showInputDialog(testName, new ITextInputCallback() {
                            @Override
                            public void onTextEntered(String text) {
                                APDUData apduData = createAPDUData("03A01D000005" + text);

                                Disposable disposable = exchangeAPDUObservable(apduData)
                                        .firstElement()
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribeOn(Schedulers.io())
                                        .subscribe(result -> {
                                            boolean success = result.endsWith("9000");
                                            logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                                        }, throwable -> disconnectOnException(testName, throwable));
                                compositeDisposable.add(disposable);
                            }

                            @Override
                            public void onCancel() {
                                logReceivedMessage(testName + " cancelled");
                            }
                });

        APDUData apduData = createAPDUData("03A01D000005AABBBBBBBB");

        Disposable disposable = exchangeAPDUObservable(apduData)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    boolean success = result.endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight816() {
        String testName = "816: Lock Signature ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        showInputDialog(testName, new ITextInputCallback() {
            @Override
            public void onTextEntered(String text) {
                APDUData apduData = createAPDUData("03A01D000001" + text);

                Disposable disposable = exchangeAPDUObservable(apduData)
                        .firstElement()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(result -> {
                            boolean success = result.endsWith("9000");
                            logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                        }, throwable -> disconnectOnException(testName, throwable));
                compositeDisposable.add(disposable);
            }

            @Override
            public void onCancel() {
                logReceivedMessage(testName + " cancelled");
            }
        });
    }

    private void ultralight817() {
        logReceivedMessage("not implemented");
    }

    private void ultralight818() {
        String testName = "818: Test Multiple APDUs, No OK List ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        List<APDUData> apduDataList = new ArrayList<>();
        apduDataList.add(createAPDUData("03A01A00000101"));
        apduDataList.add(createAPDUData("03A01B0000050101000000"));
        apduDataList.add(createAPDUData("03A01A00000101"));

        Disposable disposable = exchangeAPDUListObservable(
                apduDataList,
                testName,
                false,
                true)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(list -> {
                    boolean success = list != null
                            && list.size() == 3
                            && list.get(0).endsWith("9000")
                            && list.get(1).endsWith("9000")
                            && list.get(2).endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight819() {
        String testName = "819: Test Multiple APDUs with an OK List ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        List<APDUData> apduDataList = new ArrayList<>();
        apduDataList.add(createAPDUData("03A01A00000101", "91009000"));
        apduDataList.add(createAPDUData("03A01B0000050101000000", "91009000"));
        apduDataList.add(createAPDUData("03A01A00000101", "91009000"));

        Disposable disposable = exchangeAPDUListObservable(
                apduDataList,
                testName,
                false,
                true)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(list -> {
                    boolean success = list != null
                            && list.size() == 3
                            && list.get(0).endsWith("9000")
                            && list.get(1).endsWith("9000")
                            && list.get(2).endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight820() {
        String testName = "820: Test Multiple APDUs with failure on OK List ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        List<APDUData> apduDataList = new ArrayList<>();
        apduDataList.add(createAPDUData("03A01A00000101", "91009000"));
        apduDataList.add(createAPDUData("03A01B0000050101000000", "9100"));
        apduDataList.add(createAPDUData("03A01A00000101", "90009100"));

        Disposable disposable = exchangeAPDUListObservable(
                apduDataList,
                testName,
                false,
                true)
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(list -> {
                    boolean success = list != null
                            && list.size() == 2
                            && list.get(0).endsWith("9000")
                            && list.get(1).endsWith("9000");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }

    private void ultralight830() {
        String testName = "830: Authenticate Failure ";
        logReceivedMessage("===============================");
        logReceivedMessage(testName);

        APDUData firstCommand = createAPDUData("03A02000000100");
        Disposable disposable = exchangeAPDUObservable(firstCommand)
                .flatMap(response -> {
                    if (response.endsWith("9000")) {
                        APDUData secondCommand = createAPDUData("03A02100001000112233445566778899aabbccddeeff");
                        return exchangeAPDUObservable(secondCommand);
                    } else {
                        return Observable.just(response);
                    }
                })
                .firstElement()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .doFinally(this::disconnectFromCard)
                .subscribe(result -> {
                    boolean success = result.endsWith("91AE");
                    logReceivedMessage(testName + (success ? "Passed" : "Failed"));
                }, throwable -> disconnectOnException(testName, throwable));
        compositeDisposable.add(disposable);
    }
    //---------------------------------------------------------------------------------------
    //endregion

    //region misc functions
    //---------------------------------------------------------------------------------------
    protected Observable<String> authenticateObservable() {
        APDUData authenticate = createAPDUData("039071000002030000", "91AF");
        return exchangeAPDUObservable(authenticate);
    }

    private void disconnectOnException(String testName, Throwable throwable) {
        logReceivedMessage(testName + "Failed");
        throwable.printStackTrace();
        disconnectFromCard();
    }
    //---------------------------------------------------------------------------------------
    //endregion
}
