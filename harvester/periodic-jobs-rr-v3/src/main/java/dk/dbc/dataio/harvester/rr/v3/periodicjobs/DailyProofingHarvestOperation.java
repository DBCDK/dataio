package dk.dbc.dataio.harvester.rr.v3.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterRecord;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.PeriodicJobsV3HarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepo3Connector;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Specialized harvest operation for daily proofing records
 */
public class DailyProofingHarvestOperation extends HarvestOperation {
    public DailyProofingHarvestOperation(PeriodicJobsV3HarvesterConfig config,
                                         BinaryFileStore binaryFileStore,
                                         FileStoreServiceConnector fileStoreServiceConnector,
                                         FlowStoreServiceConnector flowStoreServiceConnector,
                                         JobStoreServiceConnector jobStoreServiceConnector,
                                         WeekResolverConnector weekResolverConnector,
                                         FbiInfoConnector fbiInfoConnector,
                                         ManagedExecutorService executor) {
        this(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector,
                weekResolverConnector, fbiInfoConnector, executor, null);
    }

    DailyProofingHarvestOperation(PeriodicJobsV3HarvesterConfig config,
                                  BinaryFileStore binaryFileStore,
                                  FileStoreServiceConnector fileStoreServiceConnector,
                                  FlowStoreServiceConnector flowStoreServiceConnector,
                                  JobStoreServiceConnector jobStoreServiceConnector,
                                  WeekResolverConnector weekResolverConnector,
                                  FbiInfoConnector fbiInfoConnector,
                                  ManagedExecutorService executor,
                                  RawRepo3Connector rawRepoConnector) {
        super(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector,
                weekResolverConnector, fbiInfoConnector, executor, rawRepoConnector);
    }

    @Override
    RecordFetcher getRecordFetcher(RecordIdDTO recordId, RecordServiceConnector recordServiceConnector,
                                   PeriodicJobsV3HarvesterConfig config) {
        return new RecordFetcher(recordId, recordServiceConnector, config);
    }

    @SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")
    static class RecordFetcher extends dk.dbc.dataio.harvester.rr.v3.periodicjobs.RecordFetcher {
        private static final Logger LOGGER = LoggerFactory.getLogger(RecordFetcher.class);

        public RecordFetcher(RecordIdDTO recordId, RecordServiceConnector recordServiceConnector,
                             PeriodicJobsV3HarvesterConfig config) {
            super(recordId, recordServiceConnector, config);
        }

        @Override
        HarvesterRecord getAddiContent(AddiMetaData addiMetaData) throws HarvesterException {
            final Map<String, RecordEntryDTO> records;
            try {
                records = fetchRecordCollectionDataIO(recordId, true, true, true);
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

            final RecordEntryDTO recordData = records.get(recordId.getBibliographicRecordId());

            DBCTrackedLogContext.setTrackingId(recordData.getTrackingId());

            LOGGER.info("Fetched record collection for {}", recordId);

            addiMetaData
                    .withTrackingId(recordData.getTrackingId())
                    .withCreationDate(getRecordCreationDate(recordData))
                    .withSubmitterNumber(resolveAgencyId(recordData))
                    .withEnrichmentTrail(recordData.getEnrichmentTrail())
                    .withFormat(config.getContent().getFormat());

            return toMarcJSonCollection(records);
        }
    }

}
