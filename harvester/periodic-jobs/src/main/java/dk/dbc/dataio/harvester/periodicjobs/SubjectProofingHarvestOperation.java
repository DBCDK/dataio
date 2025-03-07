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
import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Specialized harvest operation for subject proofing records
 */
public class SubjectProofingHarvestOperation extends HarvestOperation {
    public SubjectProofingHarvestOperation(PeriodicJobsHarvesterConfig config,
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

    SubjectProofingHarvestOperation(PeriodicJobsHarvesterConfig config,
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
        public AddiRecord call() throws HarvesterException {
            final AddiMetaData addiMetaData = new AddiMetaData()
                    .withBibliographicRecordId(recordId.getBibliographicRecordId());
            try {
                // Firstly retrieve the collection containing the subject proofing record
                final MarcExchangeCollection marcExchangeCollection =
                        (MarcExchangeCollection) getAddiContent(addiMetaData);

                // Secondly extract the bibliographic record ID from 670*b
                // then fetch and add the bibliographic record collection
                final String bibliographicRecordId = getBibliographicRecordId(marcExchangeCollection);
                if (bibliographicRecordId != null) {
                    final Map<String, RecordDTO> bibliographicRecordCollection =
                            fetchRecordCollection(new RecordIdDTO(bibliographicRecordId, recordId.getAgencyId()));
                    for (RecordDTO recordData : bibliographicRecordCollection.values()) {
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

        private String getBibliographicRecordId(MarcExchangeCollection marcExchangeCollection) {
            for (MarcRecord marcRecord : marcExchangeCollection.getRecords()) {
                final Optional<String> f670a = marcRecord.getSubFieldValue("670", 'a');
                if (f670a.isPresent()) {
                    return f670a.get();
                }
            }
            return null;
        }
    }
}
