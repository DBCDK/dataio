/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.HarvesterXmlRecord;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.weekresolver.WeekResolverConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.concurrent.ManagedExecutorService;
import java.util.Map;

/**
 * Specialized harvest operation for daily proofing records
 */
public class DailyProofingHarvestOperation extends HarvestOperation {
    public DailyProofingHarvestOperation(PeriodicJobsHarvesterConfig config,
                                         BinaryFileStore binaryFileStore,
                                         FileStoreServiceConnector fileStoreServiceConnector,
                                         FlowStoreServiceConnector flowStoreServiceConnector,
                                         JobStoreServiceConnector jobStoreServiceConnector,
                                         WeekResolverConnector weekResolverConnector,
                                         ManagedExecutorService executor) {
        this(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector,
                weekResolverConnector, executor, null);
    }

    DailyProofingHarvestOperation(PeriodicJobsHarvesterConfig config,
                                  BinaryFileStore binaryFileStore,
                                  FileStoreServiceConnector fileStoreServiceConnector,
                                  FlowStoreServiceConnector flowStoreServiceConnector,
                                  JobStoreServiceConnector jobStoreServiceConnector,
                                  WeekResolverConnector weekResolverConnector,
                                  ManagedExecutorService executor,
                                  RawRepoConnector rawRepoConnector) {
        super(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector,
                weekResolverConnector, executor, rawRepoConnector);
    }

    @Override
    RecordFetcher getRecordFetcher(RecordIdDTO recordId, RecordServiceConnector recordServiceConnector,
                                   PeriodicJobsHarvesterConfig config) {
        return new RecordFetcher(recordId, recordServiceConnector, config);
    }

    static class RecordFetcher extends dk.dbc.dataio.harvester.periodicjobs.RecordFetcher {
        private static final Logger LOGGER = LoggerFactory.getLogger(RecordFetcher.class);

        public RecordFetcher(RecordIdDTO recordId, RecordServiceConnector recordServiceConnector,
                             PeriodicJobsHarvesterConfig config) {
            super(recordId, recordServiceConnector, config);
        }

        @Override
        HarvesterXmlRecord getAddiContent(AddiMetaData addiMetaData)
                throws HarvesterException {
            final Map<String, RecordDTO> records;
            try {
                records = fetchRecordCollectionDataIO(recordId, true, true);
            } catch (HarvesterSourceException e) {
                throw new HarvesterSourceException("Unable to fetch record collection for " +
                        recordId + ": " + e.getMessage(), e);
            }
            if (records.isEmpty()) {
                throw new HarvesterInvalidRecordException("Empty record collection returned for " +
                        recordId);
            }
            if (!records.containsKey(recordId.getBibliographicRecordId())) {
                throw new HarvesterInvalidRecordException(String.format(
                        "Record %s was not found in returned collection", recordId));
            }

            final RecordDTO recordData = records.get(recordId.getBibliographicRecordId());

            DBCTrackedLogContext.setTrackingId(recordData.getTrackingId());

            LOGGER.info("Fetched record collection for {}", recordId);

            addiMetaData
                    .withTrackingId(recordData.getTrackingId())
                    .withCreationDate(getRecordCreationDate(recordData))
                    .withSubmitterNumber(resolveAgencyId(recordData))
                    .withEnrichmentTrail(recordData.getEnrichmentTrail())
                    .withFormat(config.getContent().getFormat());

            return createMarcExchangeCollection(records);
        }
    }

}
