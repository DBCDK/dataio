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
import dk.dbc.weekresolver.WeekResolverConnector;

import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Specialized harvest operation for only records which doesn't have holdings
 */
public class RecordsWithoutHoldingsHarvestOperation extends HarvestOperation {
    private final HoldingsItemsConnector holdingsItemsConnector;

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
                final Set<Integer> agenciesWithHoldings = holdingsItemsConnector
                        .hasHoldings(recordId.getBibliographicRecordId(), Set.of(870970));

                // If zero is returned and WITH_HOLDINGS is specified no job is created
                if (config.getContent().getHoldingsFilter() == PeriodicJobsHarvesterConfig.HoldingsFilter.WITH_HOLDINGS &&
                        agenciesWithHoldings.isEmpty()) {
                    return null;
                }

                // If zero is returned and WITHOUT_HOLDINGS is specified job is created
                if (config.getContent().getHoldingsFilter() == PeriodicJobsHarvesterConfig.HoldingsFilter.WITHOUT_HOLDINGS &&
                        !agenciesWithHoldings.isEmpty()) {
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