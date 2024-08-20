package co.poynt.samples.dcatestapp.utils;

import co.poynt.os.model.APDUData;
import co.poynt.os.model.ConnectionOptions;
import co.poynt.os.services.v1.IPoyntCardReaderService;
import co.poynt.os.services.v1.IPoyntConfigurationService;

public interface IHelper {
    IPoyntCardReaderService getCardReaderService();

    IPoyntConfigurationService getPoyntConfigurationService();

    void updateConnectionOptionsInterface(ConnectionOptions connectionOptions);

    void updateAPDUDataInterface(APDUData apduData);

    void logReceivedMessage(String message);
}
