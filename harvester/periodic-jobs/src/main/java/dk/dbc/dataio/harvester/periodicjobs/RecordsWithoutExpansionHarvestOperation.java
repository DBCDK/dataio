package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.libcore.DBC;
import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Specialized harvest operation without authority record expansion
 */
public class RecordsWithoutExpansionHarvestOperation extends HarvestOperation {
    public RecordsWithoutExpansionHarvestOperation(PeriodicJobsHarvesterConfig config,
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

    RecordsWithoutExpansionHarvestOperation(PeriodicJobsHarvesterConfig config,
                                            BinaryFileStore binaryFileStore,
                                            FileStoreServiceConnector fileStoreServiceConnector,
                                            FlowStoreServiceConnector flowStoreServiceConnector,
                                            JobStoreServiceConnector jobStoreServiceConnector,
                                            WeekResolverConnector weekResolverConnector,
                                            FbiInfoConnector fbiInfoConnector,
                                            ManagedExecutorService executor,
                                            RawRepoConnector rawRepoConnector) {
        super(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector,
                weekResolverConnector, fbiInfoConnector, executor, rawRepoConnector);
    }

    @Override
    RecordFetcher getRecordFetcher(RecordIdDTO recordId, RecordServiceConnector recordServiceConnector,
                                   PeriodicJobsHarvesterConfig config) {
        return new RecordFetcher(recordId, recordServiceConnector, config);
    }

    @SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")
    static class RecordFetcher extends dk.dbc.dataio.harvester.periodicjobs.RecordFetcher {
        private static final Logger LOGGER = LoggerFactory.getLogger(RecordFetcher.class);

        public RecordFetcher(RecordIdDTO recordId, RecordServiceConnector recordServiceConnector,
                             PeriodicJobsHarvesterConfig config) {
            super(recordId, recordServiceConnector, config);
        }

        @Override
        Map<String, RecordDTO> fetchRecordCollection(RecordIdDTO recordId)
                throws HarvesterSourceException {
            try {
                final RecordServiceConnector.Params params = new RecordServiceConnector.Params()
                        .withUseParentAgency(false)
                        .withExcludeAutRecords(false)
                        .withAllowDeleted(true)
                        .withExpand(false);
                if (recordId.getAgencyId() == DBC.agency.toInt()) {
                    params.withUseParentAgency(true);
                }
                final HashMap<String, RecordDTO> recordDataCollection =
                        recordServiceConnector.getRecordDataCollection(recordId, params);

                if (recordDataCollection == null) {
                    return Collections.emptyMap();
                }
                return recordDataCollection;
            } catch (RecordServiceConnectorException e) {
                throw new HarvesterSourceException("Unable to fetch record collection for " +
                        recordId.getAgencyId() + ":" + recordId.getBibliographicRecordId() + " " +
                        e.getMessage(), e);
            }
        }
    }

}
