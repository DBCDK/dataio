/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.util.AddiUtil;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddiRecordsToItemWrapper {

    private enum AddiStatus {OK, FAILED_STACKTRACE, FAILED_VALIDATION}
    private StringBuilder crossAddiRecordsMessage = new StringBuilder();
    private AddiRecordPreprocessor addiRecordPreprocessor;
    private OpenUpdateServiceConnector openUpdateServiceConnector;
    private ChunkItem processedChunkItem;
    private UpdateRecordResultMarshaller updateRecordResultMarshaller;

    private int addiRecordIndex;
    private int totalNumberOfAddiRecords;
    private String trackingId;
    private List<Diagnostic> diagnostics = new ArrayList<>();

    /**
     * @param processedChunkItem processed Chunk Item to copy values from
     * @param addiRecordPreprocessor ADDI record pre-processor
     * @param openUpdateServiceConnector OpenUpdate webservice connector
     * @param updateRecordResultMarshaller updateRecordResultMarshaller
     * @throws NullPointerException NullPointer thrown if arguments are null
     */
    public AddiRecordsToItemWrapper(ChunkItem processedChunkItem, AddiRecordPreprocessor addiRecordPreprocessor,
                                    OpenUpdateServiceConnector openUpdateServiceConnector,
                                    UpdateRecordResultMarshaller updateRecordResultMarshaller) throws NullPointerException {

        this.processedChunkItem = InvariantUtil.checkNotNullOrThrow(processedChunkItem, "processedChunkItem");
        this.addiRecordPreprocessor = InvariantUtil.checkNotNullOrThrow(addiRecordPreprocessor, "addiRecordPreprocessor");
        this.openUpdateServiceConnector = InvariantUtil.checkNotNullOrThrow(openUpdateServiceConnector, "openUpdateServiceConnector");
        this.updateRecordResultMarshaller = InvariantUtil.checkNotNullOrThrow(updateRecordResultMarshaller, "updateRecordResultMarshaller");
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
            return ObjectFactory.buildFailedChunkItem(
                    processedChunkItem.getId(),
                    "Failed when reading Addi records for processed ChunkItem: " + processedChunkItem.getId() + " -> " + StringUtil.getStackTraceString(t));
        }

        final Optional<AddiStatus> failed = addiRecordsForItem.stream()
                // retrieve the AddiStatus from each call to OpenUpdate
                .map(addiRecord -> callOpenUpdateWebServiceForAddiRecordAndBuildItemContent(addiRecord, addiRecordsForItem.indexOf(addiRecord)))
                // only collect the failed status'
                .filter(addiStatus -> addiStatus == AddiStatus.FAILED_STACKTRACE || addiStatus == AddiStatus.FAILED_VALIDATION)
                // retrieve the first -> if a failed status exist the Optional object has a present object associated with it
                .findFirst();

        ChunkItem chunkItem = ObjectFactory.buildSuccessfulChunkItem(processedChunkItem.getId(),
                getItemContentCrossAddiRecords(), ChunkItem.Type.STRING);

        if(failed.isPresent()) {
            diagnostics.stream().forEach(chunkItem::appendDiagnostics);
        }
        return chunkItem;
    }

    /*
     * Private methods
     */

    private AddiStatus callOpenUpdateWebServiceForAddiRecordAndBuildItemContent(AddiRecord addiRecord, int addiRecordIndex) {
        this.addiRecordIndex = addiRecordIndex + 1;
        try {
            final AddiRecordPreprocessor.Result preprocessorResult = addiRecordPreprocessor.preprocess(addiRecord);
            trackingId = preprocessorResult.getTrackingId();

            final UpdateRecordResult webserviceResult = openUpdateServiceConnector.updateRecord(
                    preprocessorResult.getSubmitter(),
                    preprocessorResult.getTemplate(),
                    preprocessorResult.getBibliographicRecord(),
                    trackingId);

            if(webserviceResult.getUpdateStatus() == UpdateStatusEnum.OK) {
                crossAddiRecordsMessage.append(getAddiRecordMessage(AddiStatus.OK));
                return AddiStatus.OK;
            }
           else {
                crossAddiRecordsMessage.append(getAddiRecordMessage(AddiStatus.FAILED_VALIDATION));
                crossAddiRecordsMessage.append(updateRecordResultMarshaller.asXml(webserviceResult));

                UpdateRecordErrorInterpreter updateRecordErrorInterpreter = new UpdateRecordErrorInterpreter();
                List<Diagnostic> errorDiagnostics = updateRecordErrorInterpreter.getDiagnostics(webserviceResult, addiRecord.getContentData());
                diagnostics.addAll(errorDiagnostics);

                return AddiStatus.FAILED_VALIDATION;
            }
        } catch (Throwable t) {
            crossAddiRecordsMessage.append(getAddiRecordMessage(AddiStatus.FAILED_STACKTRACE));
            crossAddiRecordsMessage.append(StringUtil.getStackTraceString(t));
            diagnostics.add(buildDiagnosticForGenericUpdateRecordError(t));
            return AddiStatus.FAILED_STACKTRACE;
        }
    }

    private Diagnostic buildDiagnosticForGenericUpdateRecordError(Throwable t) {
        final String diagnosticMessage = t.getMessage() != null ? t.getMessage() : t.getClass().getCanonicalName()
                + " occurred while calling openUpdateService";
        return ObjectFactory.buildFatalDiagnostic(diagnosticMessage, t);
    }

    private String getAddiRecordMessage(AddiStatus addiStatus) {
        final String itemResultTemplate = "Addi record with OpenUpdate trackingID %s : %s out of %s -> %s \\n";
        return String.format(itemResultTemplate, trackingId, addiRecordIndex, totalNumberOfAddiRecords, addiStatus);
    }

    private String getItemContentCrossAddiRecords() {
        return this.crossAddiRecordsMessage.toString();
    }
}
