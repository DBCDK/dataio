package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.openupdate.mapping.OpenUpdateResponseDTO;
import dk.dbc.dataio.sink.openupdate.mapping.UpdateRecordResponseMapper;
import dk.dbc.dataio.sink.util.ExceptionUtil;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ThomasBerg on 20/10/15.
 */
public class ItemToAddiRecordsWrapper {

    private enum AddiStatus {OK, FAILED_STACKTRACE, FAILED_VALIDATION};
    public enum ItemStatus {OK, FAILED};

    private final List<AddiRecord> addiRecordsForItem;
    private StringBuilder crossAddiRecordsMessage = new StringBuilder();
    private OpenUpdateServiceConnector openUpdateServiceConnector;

    public ItemToAddiRecordsWrapper(List<AddiRecord> addiRecordsForItem, OpenUpdateServiceConnector openUpdateServiceConnector) throws NullPointerException {

        InvariantUtil.checkNotNullOrThrow(addiRecordsForItem, "addiRecordsForItem");
        InvariantUtil.checkNotNullOrThrow(openUpdateServiceConnector, "openUpdateServiceConnector");

        this.addiRecordsForItem = addiRecordsForItem;
        this.openUpdateServiceConnector = openUpdateServiceConnector;
    }

    public ItemStatus callOpenUpdateWebServiceForEachAddiRecord() {
        int currentAddiRercordIndex = 1;

        List<AddiStatus> listOfAddiStatus = new ArrayList<>();
        for(AddiRecord addiRecord : addiRecordsForItem) {

            listOfAddiStatus.add(callOpenUpdateWebServiceForAddiRecord(addiRecord, currentAddiRercordIndex));
            currentAddiRercordIndex++;
        }

        // If just one of the the calls to OpenUpdate web service failed then Item status is FAILED
        for(AddiStatus addiStatus : listOfAddiStatus) {
            if(addiStatus != AddiStatus.OK) {
                return ItemStatus.FAILED;
            }
        }

        return ItemStatus.OK;
    }

    public String getItemContent() {
        return this.crossAddiRecordsMessage.toString();
    }

    private AddiStatus callOpenUpdateWebServiceForAddiRecord(AddiRecord addiRecord, int addiRecordIndex) {
        try {
            AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor(addiRecord);

            final UpdateRecordResult webserviceResult = openUpdateServiceConnector.updateRecord(
                    addiRecordPreprocessor.getTemplate(),
                    addiRecordPreprocessor.getMarcXChangeRecord());
            final OpenUpdateResponseDTO mappedWebServiceResult = new UpdateRecordResponseMapper<UpdateRecordResult>(webserviceResult).map();

            if(mappedWebServiceResult.getStatus() == OpenUpdateResponseDTO.Status.OK) {
                crossAddiRecordsMessage.append( getAddiRecordMessage(addiRecordIndex, AddiStatus.OK.toString()) );
                return AddiStatus.OK;
            } else {
                crossAddiRecordsMessage.append( getAddiRecordMessage(addiRecordIndex, AddiStatus.FAILED_VALIDATION.toString()) + mappedWebServiceResult.asXml() );
                return AddiStatus.FAILED_VALIDATION;
            }


        } catch (Throwable t) {
            crossAddiRecordsMessage.append( getAddiRecordMessage(addiRecordIndex, AddiStatus.FAILED_STACKTRACE.toString()) + ExceptionUtil.stackTraceAsString(t) );
            return AddiStatus.FAILED_STACKTRACE;
        }
    }

    private String getAddiRecordMessage(int addiRecordIndex, String addiStatus) {
        final String itemResultTemplate = "Addi record: %s out of %s -> %s \\n";
        return String.format(itemResultTemplate, addiRecordIndex, addiRecordsForItem.size(), addiStatus);
    }

}
