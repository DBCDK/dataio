package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.openupdate.mapping.OpenUpdateResponseDTO;
import dk.dbc.dataio.sink.openupdate.mapping.UpdateRecordResponseMapper;
import dk.dbc.dataio.sink.util.AddiUtil;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;

import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.commons.types.ChunkItem.Status.FAILURE;
import static dk.dbc.dataio.commons.types.ChunkItem.Status.SUCCESS;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.asBytes;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.getStackTraceAsString;

/**
 * Created by ThomasBerg on 20/10/15.
 */
public class AddiRecordsToItemWrapper {

    private enum AddiStatus {OK, FAILED_STACKTRACE, FAILED_VALIDATION}
    private StringBuilder crossAddiRecordsMessage = new StringBuilder();
    private OpenUpdateServiceConnector openUpdateServiceConnector;
    private ChunkItem processedChunkItem;

    private int addiRecordIndex;
    private int totalNumberOfAddiRecords;

    /**
     *
     * @param processedChunkItem            processed Chunk Item to copy values from
     * @param openUpdateServiceConnector    OpenUpdate webservice connector
     * @throws NullPointerException         NullPointer thrown if arguments are null
     */
    public AddiRecordsToItemWrapper(ChunkItem processedChunkItem, OpenUpdateServiceConnector openUpdateServiceConnector) throws NullPointerException {

        InvariantUtil.checkNotNullOrThrow(processedChunkItem, "processedChunkItem");
        InvariantUtil.checkNotNullOrThrow(openUpdateServiceConnector, "openUpdateServiceConnector");

        this.processedChunkItem = processedChunkItem;
        this.openUpdateServiceConnector = openUpdateServiceConnector;
    }

    /**
     * calls the openupdate web service for all Addi records and concatenate all the result to a single result in the ChunkItem data part.
     * @return  returns the ChunkItem ready to store in JobStore.
     */
    public ChunkItem callOpenUpdateWebServiceForEachAddiRecord() {
        addiRecordIndex = 1;
        List<AddiRecord> addiRecordsForItem;
        try {
            addiRecordsForItem = AddiUtil.getAddiRecordsFromChunkItem(processedChunkItem);
            totalNumberOfAddiRecords = addiRecordsForItem.size();
        } catch (Throwable t) {
            return new ChunkItem(
                    processedChunkItem.getId(),
                    asBytes("Failed when reading Addi records for processed ChunkItem: " + processedChunkItem.getId() + " -> " + getStackTraceAsString(t)),
                    FAILURE );
        }


        List<AddiStatus> listOfAddiStatus = new ArrayList<>();
        for(AddiRecord addiRecord : addiRecordsForItem) {

            listOfAddiStatus.add(callOpenUpdateWebServiceForAddiRecordAndBuildItemContent(addiRecord));
            addiRecordIndex++;
        }

        // If just one of the the calls to OpenUpdate web service failed then Item status is FAILED
        ChunkItem.Status itemStatus = SUCCESS;
        for(AddiStatus addiStatus : listOfAddiStatus) {
            if(addiStatus != AddiStatus.OK) {
                itemStatus = FAILURE;
                break;
            }
        }
        return new ChunkItem(processedChunkItem.getId(), asBytes(this.getItemContentCrossAddiRecords()), itemStatus);
    }


    // Package scoped for test reasons - originally private visibility.
    AddiStatus callOpenUpdateWebServiceForAddiRecordAndBuildItemContent(AddiRecord addiRecord) {
        try {
            AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor(addiRecord);

            final UpdateRecordResult webserviceResult = openUpdateServiceConnector.updateRecord(
                    addiRecordPreprocessor.getTemplate(),
                    addiRecordPreprocessor.getMarcXChangeRecord());
            final OpenUpdateResponseDTO mappedWebServiceResult = new UpdateRecordResponseMapper<UpdateRecordResult>(webserviceResult).map();

            if(mappedWebServiceResult.getStatus() == OpenUpdateResponseDTO.Status.OK) {
                crossAddiRecordsMessage.append( getAddiRecordMessage(AddiStatus.OK) );
                return AddiStatus.OK;
            } else {
                crossAddiRecordsMessage.append( getAddiRecordMessage(AddiStatus.FAILED_VALIDATION) + mappedWebServiceResult.asXml() );
                return AddiStatus.FAILED_VALIDATION;
            }


        } catch (Throwable t) {
            crossAddiRecordsMessage.append( getAddiRecordMessage(AddiStatus.FAILED_STACKTRACE) + getStackTraceAsString(t) );
            return AddiStatus.FAILED_STACKTRACE;
        }
    }

    private String getAddiRecordMessage(AddiStatus addiStatus) {
        final String itemResultTemplate = "Addi record: %s out of %s -> %s \\n";
        return String.format(itemResultTemplate, addiRecordIndex, totalNumberOfAddiRecords, addiStatus);
    }

    private String getItemContentCrossAddiRecords() {
        return this.crossAddiRecordsMessage.toString();
    }
}
