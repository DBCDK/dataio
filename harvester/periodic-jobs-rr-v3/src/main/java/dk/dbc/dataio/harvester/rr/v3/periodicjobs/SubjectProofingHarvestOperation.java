package dk.dbc.dataio.harvester.rr.v3.periodicjobs;

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
import dk.dbc.dataio.harvester.types.MarcJSonCollection;
import dk.dbc.dataio.harvester.types.PeriodicJobsV3HarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepo3Connector;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Specialized harvest operation for subject proofing records
 */
public class SubjectProofingHarvestOperation extends HarvestOperation {
    public SubjectProofingHarvestOperation(PeriodicJobsV3HarvesterConfig config,
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

    SubjectProofingHarvestOperation(PeriodicJobsV3HarvesterConfig config,
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
        public AddiRecord call() throws HarvesterException {
            final AddiMetaData addiMetaData = new AddiMetaData()
                    .withBibliographicRecordId(recordId.getBibliographicRecordId());
            try {
                // Firstly, retrieve the collection containing the subject proofing record
                final MarcJSonCollection marcJSonCollection =
                        (MarcJSonCollection) getAddiContent(addiMetaData);

                // Secondly, extract the bibliographic record ID from 670*b
                // then fetch and add the bibliographic record collection
                final String bibliographicRecordId = getBibliographicRecordId(marcJSonCollection);
                if (bibliographicRecordId != null) {
                    final Map<String, RecordEntryDTO> bibliographicRecordCollection =
                            fetchRecordCollection(new RecordIdDTO(bibliographicRecordId, recordId.getAgencyId()));
                    for (RecordEntryDTO recordData : bibliographicRecordCollection.values()) {
                        LOGGER.debug("Adding {} member to {} marcxchange collection",
                                recordData.getRecordId(), recordId);
                        marcJSonCollection.addMember(getRecordContent(recordData.getRecordId(), recordData));
                    }
                }

                return createAddiRecord(addiMetaData, marcJSonCollection.asBytes());
            } catch (HarvesterInvalidRecordException | HarvesterSourceException e) {
                final String errorMsg = String.format("Harvesting RawRepo %s failed: %s", recordId, e.getMessage());
                LOGGER.error(errorMsg);

                return createAddiRecord(
                        addiMetaData.withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, errorMsg)), null);
            } finally {
                DBCTrackedLogContext.remove();
            }
        }

        private String getBibliographicRecordId(MarcJSonCollection marcXchangeCollection) {
            for (MarcBinding marcBinding : marcXchangeCollection.getRecords()) {
                final List<String> f670a = marcBinding.getSubFieldValues("670", 'a');
                if (!f670a.isEmpty()) {
                    return f670a.getFirst();
                }
            }
            return null;
        }
    }
}
