/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

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
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.rawrepo.RecordData;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.weekresolver.WeekResolverConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.concurrent.ManagedExecutorService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    RecordFetcher getRecordFetcher(RecordId recordId, RecordServiceConnector recordServiceConnector,
                                   PeriodicJobsHarvesterConfig config) {
        return new RecordFetcher(recordId, recordServiceConnector, config);
    }

    static class RecordFetcher extends dk.dbc.dataio.harvester.periodicjobs.RecordFetcher {
        private static final Logger LOGGER = LoggerFactory.getLogger(RecordFetcher.class);

        public RecordFetcher(RecordId recordId, RecordServiceConnector recordServiceConnector,
                             PeriodicJobsHarvesterConfig config) {
            super(recordId, recordServiceConnector, config);
        }

        @Override
        public AddiRecord call() throws HarvesterException {
            final AddiMetaData addiMetaData = new AddiMetaData()
                    .withBibliographicRecordId(recordId.getBibliographicRecordId());
            try {
                // Firstly retrieve the collection containing the daily proofing record
                final MarcExchangeCollection marcExchangeCollection =
                        (MarcExchangeCollection) getAddiContent(addiMetaData);

                // Secondly extract the bibliographic record ID from 520*n,
                // then fetch and add the bibliographic record (if any) to the collection
                final List<String> bibliographicRecordIds = getBibliographicRecordIds(marcExchangeCollection);
                for (String bibliographicRecordId : bibliographicRecordIds) {
                    final RecordData recordData = fetchRecord(new RecordId(bibliographicRecordId, recordId.getAgencyId()));
                    if (recordData != null) {
                        LOGGER.debug("Adding {} member to {} marc exchange collection",
                                recordData.getRecordId(), recordId);
                        marcExchangeCollection.addMember(getRecordContent(recordData));
                    }
                }

                return createAddiRecord(addiMetaData, marcExchangeCollection.asBytes());
            } catch (HarvesterInvalidRecordException | HarvesterSourceException e) {
                final String errorMsg = String.format("Harvesting RawRepo %s failed: %s", recordId, e.getMessage());
                LOGGER.error(errorMsg);

                return createAddiRecord(
                        addiMetaData.withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, errorMsg)), null);
            } finally {
                DBCTrackedLogContext.remove();
            }
        }

        private List<String> getBibliographicRecordIds(MarcExchangeCollection marcExchangeCollection) {
            final List<String> bibliographicRecordIds = new ArrayList<>();
            for (MarcRecord marcRecord : marcExchangeCollection.getRecords()) {
                final Optional<String> f520n = marcRecord.getSubFieldValue("520", 'n');
                f520n.ifPresent(bibliographicRecordIds::add);
            }
            return bibliographicRecordIds;
        }
    }
}
