package types;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;

import java.io.InputStream;
import java.util.List;

public class TestablePartitioningParam extends PartitioningParam {
    public TestablePartitioningParam(JobEntity jobEntity,
                                     FileStoreServiceConnector fileStoreServiceConnector,
                                     boolean doSequenceAnalysis,
                                     List<Diagnostic> diagnostics,
                                     RecordSplitterConstants.RecordSplitter recordSplitter,
                                     InputStream dataFileInputStream,
                                     DataPartitionerFactory.DataPartitioner dataPartitioner) {

        super(jobEntity, fileStoreServiceConnector, doSequenceAnalysis, recordSplitter);
        this.diagnostics = diagnostics;
        this.dataFileInputStream = dataFileInputStream;
        this.dataPartitioner = dataPartitioner;
    }
}

