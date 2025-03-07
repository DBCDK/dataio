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
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnectorException;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Specialized harvest operation for only records which doesn't have holdings
 */
public class RecordsWithoutHoldingsHarvestOperation extends HarvestOperation {
    private static Logger LOGGER = LoggerFactory.getLogger(RecordsWithoutHoldingsHarvestOperation.class);
    protected HoldingsItemsConnector holdingsItemsConnector;
    protected int MAX_BUF_SIZE = 200;

    public RecordsWithoutHoldingsHarvestOperation(PeriodicJobsHarvesterConfig config,
                                                  BinaryFileStore binaryFileStore,
                                                  FileStoreServiceConnector fileStoreServiceConnector,
                                                  FlowStoreServiceConnector flowStoreServiceConnector,
                                                  JobStoreServiceConnector jobStoreServiceConnector,
                                                  WeekResolverConnector weekResolverConnector,
                                                  FbiInfoConnector fbiInfoConnector,
                                                  ExecutorService executor) {
        this(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector,
                weekResolverConnector, executor, null, null, fbiInfoConnector);
    }

    RecordsWithoutHoldingsHarvestOperation(PeriodicJobsHarvesterConfig config,
                                           BinaryFileStore binaryFileStore,
                                           FileStoreServiceConnector fileStoreServiceConnector,
                                           FlowStoreServiceConnector flowStoreServiceConnector,
                                           JobStoreServiceConnector jobStoreServiceConnector,
                                           WeekResolverConnector weekResolverConnector,
                                           ExecutorService executor,
                                           RawRepoConnector rawRepoConnector,
                                           HoldingsItemsConnector holdingsItemsConnector, FbiInfoConnector fbiInfoConnector) {
        super(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector,
                weekResolverConnector, fbiInfoConnector, executor, rawRepoConnector);

        this.holdingsItemsConnector = holdingsItemsConnector != null
                ? holdingsItemsConnector
                : createHoldingsItemsConnector(config);
    }

    private HoldingsItemsConnector createHoldingsItemsConnector(PeriodicJobsHarvesterConfig config) {
        return new HoldingsItemsConnector(config.getContent().getHoldingsSolrUrl());
    }

    @Override
    public String validateQuery() throws HarvesterException {
        List<String> recIds = search();
        List<String> buffer = new ArrayList<>();
        int size = recIds.size();
        Integer  agencyId = Integer.parseInt(config.getContent().getSubmitterNumber());
        int numFound = 0;
        int progress = 0;
        for (String recId : recIds) {
            buffer.add(recId);
            if (buffer.size() >= MAX_BUF_SIZE)  {
                LOGGER.info(String.format("flushing. Progress: %d / %d", progress, size));
                numFound += flush(buffer, agencyId, config.getContent().getHoldingsFilter()).size();
            }
            progress += 1;
        }
        numFound += flush(buffer, agencyId, config.getContent().getHoldingsFilter()).size();
        return String.format("Found %d records by combined rawrepo solr search and holdingssolr search.", numFound);
    }

    protected Set<String > flush(List<String> buffer, Integer agencyId, PeriodicJobsHarvesterConfig.HoldingsFilter holdingsFilter) {
        try {
            Set<String> found = holdingsItemsConnector.getRecordHoldings(new HashSet<String>(buffer), Set.of(agencyId));
            if (holdingsFilter == PeriodicJobsHarvesterConfig.HoldingsFilter.WITH_HOLDINGS) {
                return found;
            } else {
                HashSet<String > noHoldings =  new HashSet<>(buffer);
                noHoldings.removeAll(found);
                return noHoldings;
            }
        } catch (HoldingsItemsConnectorException e){
            LOGGER.error("RecordsWithoutHoldingsHarvestOperation:", e);
        } finally {
            buffer.clear();
        }
        return Set.of();
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
