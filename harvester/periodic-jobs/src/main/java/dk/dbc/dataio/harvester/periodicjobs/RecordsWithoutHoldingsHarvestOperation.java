package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.weekresolver.connector.WeekResolverConnector;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Specialized harvest operation for only records which doesn't have holdings
 */
public class RecordsWithoutHoldingsHarvestOperation extends HarvestOperation {
    protected HoldingsItemsConnector holdingsItemsConnector;

    public RecordsWithoutHoldingsHarvestOperation(PeriodicJobsHarvesterConfig config,
                                                  BinaryFileStore binaryFileStore,
                                                  FileStoreServiceConnector fileStoreServiceConnector,
                                                  FlowStoreServiceConnector flowStoreServiceConnector,
                                                  JobStoreServiceConnector jobStoreServiceConnector,
                                                  WeekResolverConnector weekResolverConnector,
                                                  ExecutorService executor) {
        this(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector,
                weekResolverConnector, executor, null, null);
    }

    RecordsWithoutHoldingsHarvestOperation(PeriodicJobsHarvesterConfig config,
                                           BinaryFileStore binaryFileStore,
                                           FileStoreServiceConnector fileStoreServiceConnector,
                                           FlowStoreServiceConnector flowStoreServiceConnector,
                                           JobStoreServiceConnector jobStoreServiceConnector,
                                           WeekResolverConnector weekResolverConnector,
                                           ExecutorService executor,
                                           RawRepoConnector rawRepoConnector,
                                           HoldingsItemsConnector holdingsItemsConnector) {
        super(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector,
                weekResolverConnector, executor, rawRepoConnector);

        this.holdingsItemsConnector = holdingsItemsConnector != null
                ? holdingsItemsConnector
                : createHoldingsItemsConnector(config);
    }

    private HoldingsItemsConnector createHoldingsItemsConnector(PeriodicJobsHarvesterConfig config) {
        return new HoldingsItemsConnector(config.getContent().getHoldingsSolrUrl());
    }

    @Override
    public String validateQuery() throws HarvesterException {
        int found = 0;
        try (RecordIdFile recordIdFile = new RecordIdFile(searchAndPersist(getTmpFileForSearchResult()))) {
            Iterator<RecordIdDTO> recordIdDTOIterator = recordIdFile.iterator();
            if (recordIdDTOIterator.hasNext()) {
                do {
                    RecordIdDTO recordIdDTO = recordIdDTOIterator.next();
                    found = validateAndIncrement(recordIdDTO, found);

                } while (recordIdDTOIterator.hasNext());
            }
        }
        return String.format("Found %d record by combined rawrepo solr search and holdingssolr search.", found);
    }

    private int validateAndIncrement(RecordIdDTO recordIdDTO, int foundSoFar) {
        if (recordIdDTO == null) return foundSoFar;
        int submitter = Integer.parseInt(config.getContent().getSubmitterNumber());
        boolean found = holdingsItemsConnector.hasAnyHoldings(recordIdDTO.getBibliographicRecordId(), Set.of(submitter));
        if (found && config.getContent().getHoldingsFilter() == PeriodicJobsHarvesterConfig.HoldingsFilter.WITH_HOLDINGS) {
            return ++foundSoFar;
        }
        if (!found && config.getContent().getHoldingsFilter() == PeriodicJobsHarvesterConfig.HoldingsFilter.WITHOUT_HOLDINGS) {
            return ++foundSoFar;
        }
        return foundSoFar;
    }

    @Override
    RecordFetcher getRecordFetcher(RecordIdDTO recordId, RecordServiceConnector recordServiceConnector,
                                   PeriodicJobsHarvesterConfig config) {
        return new RecordFetcher(recordId, recordServiceConnector, holdingsItemsConnector, config);
    }

    @SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")
    static class RecordFetcher extends dk.dbc.dataio.harvester.periodicjobs.RecordFetcher {
        private final HoldingsItemsConnector holdingsItemsConnector;

        public RecordFetcher(RecordIdDTO recordId,
                             RecordServiceConnector recordServiceConnector,
                             HoldingsItemsConnector holdingsItemsConnector,
                             PeriodicJobsHarvesterConfig config) {
            super(recordId, recordServiceConnector, config);
            this.holdingsItemsConnector = holdingsItemsConnector;
        }


        @Override
        public AddiRecord call() throws HarvesterException {
            final AddiMetaData addiMetaData = new AddiMetaData()
                    .withBibliographicRecordId(recordId.getBibliographicRecordId());
            try {
                int submitter = Integer.parseInt(config.getContent().getSubmitterNumber());
                boolean hasHoldings = holdingsItemsConnector.hasAnyHoldings(recordId.getBibliographicRecordId(), Set.of(submitter));

                // If there are no holdings and WITH_HOLDINGS filter is specified, no addirecord is created
                if (config.getContent().getHoldingsFilter() == PeriodicJobsHarvesterConfig.HoldingsFilter.WITH_HOLDINGS &&
                        !hasHoldings) {
                    return null;
                }

                // If there are holdings and WITHOUT_HOLDINGS filter is specified, no addirecord  is created
                if (config.getContent().getHoldingsFilter() == PeriodicJobsHarvesterConfig.HoldingsFilter.WITHOUT_HOLDINGS &&
                        hasHoldings) {
                    return null;
                }

                return createAddiRecord(addiMetaData, getAddiContent(addiMetaData).asBytes());
            } catch (HarvesterInvalidRecordException | HarvesterSourceException e) {
                final String errorMsg = String.format("Harvesting RawRepo %s failed: %s", recordId, e.getMessage());

                return createAddiRecord(
                        addiMetaData.withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, errorMsg)), null);
            } finally {
                DBCTrackedLogContext.remove();
            }
        }
    }

}
