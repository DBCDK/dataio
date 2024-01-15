package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.RecordSplitter;
import dk.dbc.dataio.commons.partioner.DataPartitioner;
import dk.dbc.dataio.commons.partioner.IncludeFilterDataPartitioner;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.dependencytracking.DefaultKeyGenerator;
import dk.dbc.dataio.jobstore.service.dependencytracking.KeyGenerator;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.invariant.InvariantUtil;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

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

    protected List<Diagnostic> diagnostics = new ArrayList<>();
    protected InputStream dataFileInputStream;
    protected DataPartitioner dataPartitioner;

    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private EntityManager entityManager;
    private JobEntity jobEntity;
    private String dataFileId;
    private KeyGenerator keyGenerator;
    private RecordSplitter recordSplitterType;
    private boolean previewOnly;

    public PartitioningParam(
            JobEntity jobEntity,
            FileStoreServiceConnector fileStoreServiceConnector,
            FlowStoreServiceConnector flowStoreServiceConnector,
            EntityManager entityManager,
            RecordSplitter recordSplitterType) throws NullPointerException {
        this(jobEntity, fileStoreServiceConnector, flowStoreServiceConnector,
                entityManager, recordSplitterType, null);
    }

    public PartitioningParam(
            JobEntity jobEntity,
            FileStoreServiceConnector fileStoreServiceConnector,
            FlowStoreServiceConnector flowStoreServiceConnector,
            EntityManager entityManager,
            RecordSplitter recordSplitterType, BitSet includeFilter) throws NullPointerException {
        this.fileStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(fileStoreServiceConnector, "fileStoreServiceConnector");
        this.flowStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(flowStoreServiceConnector, "flowStoreServiceConnector");
        this.jobEntity = InvariantUtil.checkNotNullOrThrow(jobEntity, "jobEntity");
        if (!this.jobEntity.hasFatalError()) {
            this.entityManager = InvariantUtil.checkNotNullOrThrow(entityManager, "entityManager");
            this.recordSplitterType = InvariantUtil.checkNotNullOrThrow(recordSplitterType, "recordSplitterType");
            this.keyGenerator = new DefaultKeyGenerator();
            this.dataFileId = extractDataFileIdFromURN();
            this.dataFileInputStream = newDataFileInputStream();
            this.dataPartitioner = createDataPartitioner(includeFilter);
            previewOnly = jobEntity.getSpecification().getType().canBePreview() && isSubmitterDisabled();
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

    public KeyGenerator getKeyGenerator() {
        return keyGenerator;
    }

    public void closeDataFile() {
        if (dataFileInputStream != null) {
            try {
                dataFileInputStream.close();
            } catch (IOException e) {
                LOGGER.error("Unable to close datafile input stream", e);
            }
        }
    }

    private InputStream newDataFileInputStream() {
        if (dataFileId != null && !dataFileId.isEmpty()) {
            try {
                return fileStoreServiceConnector.getFile(dataFileId);
            } catch (FileStoreServiceConnectorException | ProcessingException e) {
                final String message = String.format("Could not get input stream for data file: %s", jobEntity.getSpecification().getDataFile());
                diagnostics.add(ObjectFactory.buildFatalDiagnostic(message, e));
            }
        }
        return null;
    }

    public boolean isPreviewOnly() {
        return previewOnly;
    }

    private boolean isSubmitterDisabled() {
        final Submitter submitter;
        final long submitterId = jobEntity.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SUBMITTER).getId();

        // BAAAD IDEA!
        if ("on".equals(System.getenv("DEVELOPER"))) {
            return false;
        }
        /// END
        try {
            submitter = flowStoreServiceConnector.getSubmitter(submitterId);
            return !submitter.getContent().isEnabled();
        } catch (FlowStoreServiceConnectorException e) {
            final String message = String.format("Could not retrieve submitter: %s", submitterId);
            diagnostics.add(ObjectFactory.buildFatalDiagnostic(message, e));
        }
        return true;
    }

    private DataPartitioner createDataPartitioner(BitSet includeFilter) {
        final DataPartitioner dataPartitioner = createDataPartitioner();
        if (dataPartitioner != null && includeFilter != null) {
            return IncludeFilterDataPartitioner.newInstance(dataPartitioner, includeFilter);
        }
        return dataPartitioner;
    }

    private DataPartitioner createDataPartitioner() {
        if (dataFileInputStream != null) {
            return recordSplitterType.toPartitioner(dataFileInputStream, jobEntity.getSpecification(), jobEntity.getId(), entityManager);
        }
        return null;
    }

    private String extractDataFileIdFromURN() {
        final String dataFileURN = jobEntity.getSpecification().getDataFile();
        if (!Files.exists(Paths.get(dataFileURN))) {
            try {
                return new FileStoreUrn(dataFileURN).getFileId();
            } catch (URISyntaxException e) {
                final String message = String.format("Invalid file-store service URN: %s", dataFileURN);
                diagnostics.add(ObjectFactory.buildFatalDiagnostic(message, e));
            }
        }
        return null;
    }
}
