package types;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.partioner.DataPartitioner;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.RecordSplitter;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import jakarta.persistence.EntityManager;

import java.io.InputStream;
import java.util.List;

public class TestablePartitioningParam extends PartitioningParam {
    public TestablePartitioningParam(JobEntity jobEntity,
                                     FileStoreServiceConnector fileStoreServiceConnector,
                                     FlowStoreServiceConnector flowStoreServiceConnector,
                                     EntityManager entityManager,
                                     List<Diagnostic> diagnostics,
                                     RecordSplitter recordSplitter,
                                     InputStream dataFileInputStream,
                                     DataPartitioner dataPartitioner) {

        super(jobEntity, fileStoreServiceConnector, flowStoreServiceConnector, entityManager, recordSplitter);
        this.diagnostics = diagnostics;
        this.dataFileInputStream = dataFileInputStream;
        this.dataPartitioner = dataPartitioner;
    }
}

