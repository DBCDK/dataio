package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import jakarta.enterprise.concurrent.ManagedExecutorService;

import java.util.List;
import java.util.Set;

public class HasCoverHarvestOperation extends RecordsWithoutExpansionHarvestOperation {

    public HasCoverHarvestOperation(PeriodicJobsHarvesterConfig config, BinaryFileStore binaryFileStore, FileStoreServiceConnector fileStoreServiceConnector, FlowStoreServiceConnector flowStoreServiceConnector, JobStoreServiceConnector jobStoreServiceConnector, WeekResolverConnector weekResolverConnector, FbiInfoConnector fbiInfoConnector, ManagedExecutorService executor) {
        super(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector, weekResolverConnector, fbiInfoConnector, executor);
    }

    HasCoverHarvestOperation(PeriodicJobsHarvesterConfig config, BinaryFileStore binaryFileStore, FileStoreServiceConnector fileStoreServiceConnector, FlowStoreServiceConnector flowStoreServiceConnector, JobStoreServiceConnector jobStoreServiceConnector, WeekResolverConnector weekResolverConnector, FbiInfoConnector fbiInfoConnector, ManagedExecutorService executor, RawRepoConnector rawRepoConnector) {
        super(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector, weekResolverConnector, fbiInfoConnector, executor, rawRepoConnector);
    }

    @Override
    public List<RecordIdDTO> filter(List<RecordIdDTO> recordIds) {
        Set<RecordIdDTO> filter = fbiInfoConnector.hasCoverFilter(recordIds);
        return recordIds.stream().filter(recordId -> !filter.contains(recordId)).toList();
    }
}
