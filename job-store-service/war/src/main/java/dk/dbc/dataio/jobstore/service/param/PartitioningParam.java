package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatDataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.Iso2709DataPartitionerFactory;
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

import static dk.dbc.dataio.commons.types.RecordSplitterConstants.RecordSplitter;
import static dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory.DataPartitioner;

/**
 * This class is a parameter abstraction for the PgJobStore.addJob() method.
 * <p>
 * Parameter initialization failures will result in fatal diagnostics being added
 * to the internal diagnostics list, and the corresponding parameter field being
 * given a null value.
 * </p>
 */
public class PartitioningParam {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartitioningParam.class);

    private final FileStoreServiceConnector fileStoreServiceConnector;
    protected List<Diagnostic> diagnostics = new ArrayList<>();
    private boolean doSequenceAnalysis = Boolean.FALSE;
    private JobEntity jobEntity;
    private String dataFileId;
    protected InputStream dataFileInputStream;
    protected DataPartitioner dataPartitioner;
    private SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator;
    private RecordSplitter recordSplitterType;

    public PartitioningParam(
            JobEntity jobEntity,
            FileStoreServiceConnector fileStoreServiceConnector,
            boolean doSequenceAnalysis,
            RecordSplitter recordSplitterType) throws NullPointerException {
        this.fileStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(fileStoreServiceConnector, "fileStoreServiceConnector");
        this.jobEntity = InvariantUtil.checkNotNullOrThrow(jobEntity, "jobEntity");
        if (!this.jobEntity.hasFatalError()) {
            this.doSequenceAnalysis = doSequenceAnalysis;
            this.recordSplitterType = recordSplitterType;
            this.sequenceAnalyserKeyGenerator = newSequenceAnalyserKeyGenerator();
            this.dataFileId = extractDataFileIdFromURN();
            this.dataFileInputStream = newDataFileInputStream();
            this.dataPartitioner = newDataPartitioner();
        }
    }

    public String getDataFileId() {
        return dataFileId;
    }

    public InputStream getDataFileInputStream() {
        return dataFileInputStream;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public JobEntity getJobEntity() {
        return this.jobEntity;
    }

    public DataPartitioner getDataPartitioner() {
        return dataPartitioner;
    }

    public boolean getDoSequenceAnalysis() {
        return doSequenceAnalysis;
    }

    public RecordSplitter getRecordSplitterType() {
        return recordSplitterType;
    }

    public SequenceAnalyserKeyGenerator getSequenceAnalyserKeyGenerator() {
        return sequenceAnalyserKeyGenerator;
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

    private SequenceAnalyserKeyGenerator newSequenceAnalyserKeyGenerator() {
        if (doSequenceAnalysis) {
            if (jobEntity.getCachedSink().getSink() != null) {
                return new SequenceAnalyserSinkKeyGenerator(jobEntity.getCachedSink().getSink().getId());
            }
        } else {
            return new SequenceAnalyserNoOrderKeyGenerator();
        }
        return null;
    }

    private InputStream newDataFileInputStream() {
        if (dataFileId != null && !dataFileId.isEmpty()) {
            try {
                return fileStoreServiceConnector.getFile(dataFileId);
            } catch (FileStoreServiceConnectorException | ProcessingException e) {
                final String message = String.format("Could not get input stream for data file: %s", jobEntity.getSpecification().getDataFile());
                diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
            }
        }
        return null;
    }

    private DataPartitioner newDataPartitioner() {
        if (dataFileInputStream != null) {
            switch (recordSplitterType) {
                case XML:
                    return new DefaultXmlDataPartitionerFactory().createDataPartitioner(dataFileInputStream, jobEntity.getSpecification().getCharset());
                case ISO2709:
                    return new Iso2709DataPartitionerFactory().createDataPartitioner(dataFileInputStream, jobEntity.getSpecification().getCharset());
                case DANMARC2_LINE_FORMAT:
                    return new DanMarc2LineFormatDataPartitionerFactory().createDataPartitioner(dataFileInputStream, jobEntity.getSpecification().getCharset());
                default:
                    diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, "unknown record splitter: " + recordSplitterType));
            }
        }
        return null;
    }

    private String extractDataFileIdFromURN() {
        final String dataFileURN = jobEntity.getSpecification().getDataFile();
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