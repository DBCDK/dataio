package dk.dbc.dataio.harvester.v3;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.PeriodicJobsV3HarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepo3Connector;
import dk.dbc.libcore.DBC;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Specialized harvest operation without authority record expansion
 */
public class RecordsWithoutExpansionHarvestOperation extends HarvestOperation {
    public RecordsWithoutExpansionHarvestOperation(PeriodicJobsV3HarvesterConfig config,
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

    RecordsWithoutExpansionHarvestOperation(PeriodicJobsV3HarvesterConfig config,
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
    static class RecordFetcher extends dk.dbc.dataio.harvester.v3.RecordFetcher {
        private static final Logger LOGGER = LoggerFactory.getLogger(RecordFetcher.class);

        public RecordFetcher(RecordIdDTO recordId, RecordServiceConnector recordServiceConnector,
                             PeriodicJobsV3HarvesterConfig config) {
            super(recordId, recordServiceConnector, config);
        }

        @Override
        Map<String, RecordEntryDTO> fetchRecordCollection(RecordIdDTO recordId)
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
                List<RecordEntryDTO> recordDataCollection = recordServiceConnector.getRecordDataCollection(recordId, params);
                if (recordDataCollection == null || recordDataCollection.isEmpty()) {
                    throw new HarvesterInvalidRecordException("Record for " + recordId + " was not found");
                }
                return recordDataCollection.stream().collect(Collectors.groupingBy(e -> e.getRecordId().getBibliographicRecordId(), Collectors.reducing(null, (e1, e2) -> e1 == null ? e2 : e1)));
            } catch (RecordServiceConnectorException e) {
                throw new HarvesterSourceException("Unable to fetch record collection for " +
                        recordId.getAgencyId() + ":" + recordId.getBibliographicRecordId() + " " +
                        e.getMessage(), e);
            }
        }
    }

}
