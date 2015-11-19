package types;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class TestablePartitioningParamBuilder {
    private static FileStoreUrn fileStoreUrn ;
    static{
        try {
            fileStoreUrn = FileStoreUrn.create("42");
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private JobEntity jobEntity = new TestableJobEntityBuilder().setJobSpecification(new JobSpecificationBuilder().setDataFile(fileStoreUrn.toString()).build()).build();
    private FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private boolean doSequenceAnalysis = false;
    private String records =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<records>"
            + "<record>first</record>"
            + "<record>second</record>"
            + "<record>third</record>"
            + "<record>fourth</record>"
            + "<record>fifth</record>"
            + "<record>sixth</record>"
            + "<record>seventh</record>"
            + "<record>eighth</record>"
            + "<record>ninth</record>"
            + "<record>tenth</record>"
            + "<record>eleventh</record>"
            + "</records>";

    private List<Diagnostic> diagnostics = new ArrayList<>();
    private InputStream dataFileInputStream = new ByteArrayInputStream(records.getBytes(StandardCharsets.UTF_8));
    private RecordSplitterConstants.RecordSplitter recordSplitter = RecordSplitterConstants.RecordSplitter.XML;
    private DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory().createDataPartitioner(dataFileInputStream, StandardCharsets.UTF_8.name());

    public TestablePartitioningParamBuilder setJobEntity(JobEntity jobEntity) {
        this.jobEntity = jobEntity;
        return this;
    }

    public TestablePartitioningParamBuilder setSequenceAnalysis(boolean doSequenceAnalysis) {
        this.doSequenceAnalysis = doSequenceAnalysis;
        return this;
    }

    public TestablePartitioningParamBuilder setRecords(String records) {
        this.records = records;
        return this;
    }

    public TestablePartitioningParamBuilder setDiagnostics(List<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
        return this;
    }

    public TestablePartitioningParamBuilder setDataFileInputStream(InputStream dataFileInputStream) {
        this.dataFileInputStream = dataFileInputStream;
        return this;
    }

    public TestablePartitioningParamBuilder setRecordSplitter(RecordSplitterConstants.RecordSplitter recordSplitter) {
        this.recordSplitter = recordSplitter;
        return this;
    }

    public TestablePartitioningParamBuilder setDataPartitioner(DataPartitionerFactory.DataPartitioner dataPartitioner) {
        this.dataPartitioner = dataPartitioner;
        return this;
    }

    public TestablePartitioningParam build() {
        return new TestablePartitioningParam(jobEntity, fileStoreServiceConnector, doSequenceAnalysis, diagnostics, recordSplitter, dataFileInputStream, dataPartitioner);
    }

}
