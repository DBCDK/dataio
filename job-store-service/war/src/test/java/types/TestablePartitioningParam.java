package types;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import jakarta.persistence.EntityManager;

import java.io.InputStream;
import java.util.List;

public class TestablePartitioningParam extends PartitioningParam {
    public TestablePartitioningParam(JobEntity jobEntity,
                                     FileStoreServiceConnector fileStoreServiceConnector,
                                     FlowStoreServiceConnector flowStoreServiceConnector,
                                     EntityManager entityManager,
                                     List<Diagnostic> diagnostics,
                                     RecordSplitterConstants.RecordSplitter recordSplitter,
                                     InputStream dataFileInputStream,
                                     DataPartitioner dataPartitioner) {

        super(jobEntity, fileStoreServiceConnector, flowStoreServiceConnector, entityManager, recordSplitter);
        this.diagnostics = diagnostics;
        this.dataFileInputStream = dataFileInputStream;
        this.dataPartitioner = dataPartitioner;
    }
}

