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
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.openupdate.mapping.OpenUpdateResponseDTO;
import dk.dbc.dataio.sink.openupdate.mapping.UpdateRecordResponseMapper;
import dk.dbc.dataio.sink.util.AddiUtil;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dk.dbc.dataio.commons.types.ChunkItem.Status.FAILURE;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.asBytes;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.getStackTraceAsString;

public class AddiRecordsToItemWrapper {

    private enum AddiStatus {OK, FAILED_STACKTRACE, FAILED_VALIDATION}
    private StringBuilder crossAddiRecordsMessage = new StringBuilder();
    private OpenUpdateServiceConnector openUpdateServiceConnector;
    private ChunkItem processedChunkItem;

    private int addiRecordIndex;
    private int totalNumberOfAddiRecords;
    private UUID trackingId;

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

        final Optional<AddiStatus> failed = addiRecordsForItem.stream()
                // retrieve the AddiStatus from each call to OpenOpdate
                .map(addiRecord -> callOpenUpdateWebServiceForAddiRecordAndBuildItemContent(addiRecord, addiRecordsForItem.indexOf(addiRecord)))
                // only collect the failed status'
                .filter(addiStatus -> addiStatus == AddiStatus.FAILED_STACKTRACE || addiStatus == AddiStatus.FAILED_VALIDATION)
                // retrieve the first -> if a failed status exist the Optional object has a present object associated with it
                .findFirst();

        return new ChunkItem(
                processedChunkItem.getId(),
                asBytes(this.getItemContentCrossAddiRecords()),
                failed.isPresent() ? ChunkItem.Status.FAILURE : ChunkItem.Status.SUCCESS);
    }

    // Package scoped for test reasons - originally private visibility.
    AddiStatus callOpenUpdateWebServiceForAddiRecordAndBuildItemContent(AddiRecord addiRecord, int addiRecordIndex) {
        this.addiRecordIndex = addiRecordIndex + 1;
        trackingId = UUID.randomUUID();
        try {
            AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor(addiRecord);

            final UpdateRecordResult webserviceResult = openUpdateServiceConnector.updateRecord(
                    addiRecordPreprocessor.getTemplate(),
                    addiRecordPreprocessor.getMarcXChangeRecord(),
                    trackingId);
            final OpenUpdateResponseDTO mappedWebServiceResult = new UpdateRecordResponseMapper<UpdateRecordResult>(webserviceResult).map(trackingId);

            if(mappedWebServiceResult.getStatus() == OpenUpdateResponseDTO.Status.OK) {
                crossAddiRecordsMessage.append( getAddiRecordMessage(AddiStatus.OK) );
                return AddiStatus.OK;
            } else {
                crossAddiRecordsMessage.append(getAddiRecordMessage(AddiStatus.FAILED_VALIDATION));
                crossAddiRecordsMessage.append(mappedWebServiceResult.asXml());
                return AddiStatus.FAILED_VALIDATION;
            }
        } catch (Throwable t) {
            crossAddiRecordsMessage.append( getAddiRecordMessage(AddiStatus.FAILED_STACKTRACE));
            crossAddiRecordsMessage.append(getStackTraceAsString(t));
            return AddiStatus.FAILED_STACKTRACE;
        }
    }

    private String getAddiRecordMessage(AddiStatus addiStatus) {
        final String itemResultTemplate = "Addi record with OpenUpdate trackingID %s : %s out of %s -> %s \\n";
        return String.format(itemResultTemplate, trackingId, addiRecordIndex, totalNumberOfAddiRecords, addiStatus);
    }

    private String getItemContentCrossAddiRecords() {
        return this.crossAddiRecordsMessage.toString();
    }
}
