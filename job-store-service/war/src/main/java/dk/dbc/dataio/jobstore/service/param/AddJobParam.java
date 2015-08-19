package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserNoOrderKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is a parameter abstraction for the PgJobStore.addJob() method.
 * <p>
 * Parameter initialization failures will result in fatal diagnostics being added
 * to the internal diagnostics list, and the corresponding parameter field being
 * given a null value.
 * </p>
 */
public class AddJobParam {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddJobParam.class);

    protected final FlowStoreServiceConnector flowStoreServiceConnector;
    protected final FileStoreServiceConnector fileStoreServiceConnector;

    protected final JobInputStream jobInputStream;
    protected List<Diagnostic> diagnostics;
    protected Submitter submitter;
    protected FlowBinder flowBinder;
    protected Flow flow;
    protected Sink sink;
    protected FlowStoreReferences flowStoreReferences;
    protected SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator;
    protected String dataFileId;
    protected InputStream dataFileInputStream;
    protected DataPartitionerFactory.DataPartitioner dataPartitioner;

    public AddJobParam(
            JobInputStream jobInputStream,
            FlowStoreServiceConnector flowStoreServiceConnector,
            FileStoreServiceConnector fileStoreServiceConnector) throws NullPointerException {

        this.jobInputStream = InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
        this.flowStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(flowStoreServiceConnector, "flowStoreServiceConnector");
        this.fileStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(fileStoreServiceConnector, "fileStoreServiceConnector");
        this.diagnostics = new ArrayList<>();
        this.submitter = lookupSubmitter();
        this.flowBinder = lookupFlowBinder();
        this.flow = lookupFlow();
        this.sink = lookupSink();
        this.flowStoreReferences = newFlowStoreReferences();
        this.sequenceAnalyserKeyGenerator = newSequenceAnalyserKeyGenerator();
        this.dataFileId = extractDataFileIdFromURN();
        this.dataFileInputStream = newDataFileInputStream();
        this.dataPartitioner = newDataPartitioner();
    }

    public JobInputStream getJobInputStream() {
        return jobInputStream;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public Submitter getSubmitter() {
        return submitter;
    }

    public FlowBinder getFlowBinder() {
        return flowBinder;
    }

    public Flow getFlow() {
        return flow;
    }

    public Sink getSink() {
        return sink;
    }

    public FlowStoreReferences getFlowStoreReferences() {
        return flowStoreReferences;
    }

    public SequenceAnalyserKeyGenerator getSequenceAnalyserKeyGenerator() {
        return sequenceAnalyserKeyGenerator;
    }

    public String getDataFileId() {
        return dataFileId;
    }

    public DataPartitionerFactory.DataPartitioner getDataPartitioner() {
        return dataPartitioner;
    }

    public void closeDataFile() throws JobStoreException {
        if (dataFileInputStream != null) {
            try {
                dataFileInputStream.close();
            } catch (IOException e) {
                LOGGER.error("Unable to close datafile input stream", e);
            }
        }
    }

    private Submitter lookupSubmitter() {
        final long submitterNumber = jobInputStream.getJobSpecification().getSubmitterId();
        try {
            return flowStoreServiceConnector.getSubmitterBySubmitterNumber(submitterNumber);
        } catch(FlowStoreServiceConnectorException | ProcessingException e) {
            final String message = String.format("Could not retrieve Submitter with submitter number: %d", submitterNumber);
            diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
        }
        return null;
    }

    private FlowBinder lookupFlowBinder() {
        final JobSpecification jobSpec = jobInputStream.getJobSpecification();
        try {
            return flowStoreServiceConnector.getFlowBinder(
                    jobSpec.getPackaging(),
                    jobSpec.getFormat(),
                    jobSpec.getCharset(),
                    jobSpec.getSubmitterId(),
                    jobSpec.getDestination());
        } catch(FlowStoreServiceConnectorException | ProcessingException e) {
            final String message = String.format("Could not retrieve FlowBinder for specification: %s", jobSpec);
            diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
        }
        return null;
    }

    private Flow lookupFlow() {
        if (flowBinder != null) {
            final long flowId = flowBinder.getContent().getFlowId();
            try {
                return flowStoreServiceConnector.getFlow(flowId);
            } catch(FlowStoreServiceConnectorException | ProcessingException e) {
                final String message = String.format("Could not retrieve Flow with ID: %d", flowId);
                diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
            }
        }
        return null;
    }

    private Sink lookupSink() {
        if (flowBinder != null) {
            final long sinkId = flowBinder.getContent().getSinkId();
            try {
                return flowStoreServiceConnector.getSink(sinkId);
            } catch(FlowStoreServiceConnectorException | ProcessingException e) {
                final String message = String.format("Could not retrieve Sink with ID: %d", sinkId);
                diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
            }
        }
        return null;
    }

    private FlowStoreReferences newFlowStoreReferences() {
        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        if (flowBinder != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER,
                    new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(),flowBinder.getContent().getName()));
        }
        if (flow != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW,
                    new FlowStoreReference(flow.getId(), flow.getVersion(),flow.getContent().getName()));
        }
        if (sink != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK,
                    new FlowStoreReference(sink.getId(), sink.getVersion(),sink.getContent().getName()));
        }
        if (submitter != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER,
                    new FlowStoreReference(submitter.getId(), submitter.getVersion(),submitter.getContent().getName()));
        }
        return flowStoreReferences;
    }

    private SequenceAnalyserKeyGenerator newSequenceAnalyserKeyGenerator() {
         if (flowBinder != null) {
             if (flowBinder.getContent().getSequenceAnalysis()) {
                 if (sink != null) {
                     return new SequenceAnalyserSinkKeyGenerator(sink);
                 }
             } else {
                 return new SequenceAnalyserNoOrderKeyGenerator();
             }
         }
         return null;
    }

    private InputStream newDataFileInputStream() {
        if (dataFileId != null && !dataFileId.isEmpty()) {
            try {
                return fileStoreServiceConnector.getFile(dataFileId);
            } catch (FileStoreServiceConnectorException | ProcessingException e) {
                final String message = String.format("Could not get input stream for data file: %s",
                        jobInputStream.getJobSpecification().getDataFile());
                diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
            }
        }
        return null;
    }

    private DataPartitionerFactory.DataPartitioner newDataPartitioner() {
        if (dataFileInputStream != null) {
            return new DefaultXmlDataPartitionerFactory().createDataPartitioner(dataFileInputStream,
                    jobInputStream.getJobSpecification().getCharset());
        }
        return null;
    }

    private String extractDataFileIdFromURN() {
        final String dataFileURN = jobInputStream.getJobSpecification().getDataFile();
        if(!Files.exists(Paths.get(dataFileURN))) {
            try {
                return new FileStoreUrn(dataFileURN).getFileId();
            } catch (URISyntaxException e) {
                final String message = String.format("Invalid file-store service URN: %s", dataFileURN);
                diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
            }
        }
        return null;
    }
}
