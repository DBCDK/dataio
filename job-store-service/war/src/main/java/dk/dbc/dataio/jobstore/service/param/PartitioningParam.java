package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.dependencytracking.DefaultKeyGenerator;
import dk.dbc.dataio.jobstore.service.dependencytracking.KeyGenerator;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.partitioner.AddiDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.CsvDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatReorderingDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DsdCsvDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.IncludeFilterDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.Iso2709DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.Iso2709ReorderingDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.JsonDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.MarcXchangeAddiDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.TarredXmlDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.ViafDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.VipCsvDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.VolumeAfterParents;
import dk.dbc.dataio.jobstore.service.partitioner.VolumeIncludeParents;
import dk.dbc.dataio.jobstore.service.partitioner.ZippedXmlDataPartitioner;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.ws.rs.ProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;

import static dk.dbc.dataio.commons.types.RecordSplitterConstants.RecordSplitter;

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
            previewOnly = canBePreviewOnly() && isSubmitterDisabled();
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

    private boolean canBePreviewOnly() {
        EnumSet<JobSpecification.Type> previewSet = EnumSet.of(
                JobSpecification.Type.SUPER_TRANSIENT, JobSpecification.Type.TRANSIENT,
                JobSpecification.Type.PERSISTENT);
        return previewSet.contains(jobEntity.getSpecification().getType());
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
            switch (recordSplitterType) {
                case XML:
                    return DefaultXmlDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset());
                case ISO2709:
                    return getIso2709Partitioner();
                case ISO2709_COLLECTION:
                    return Iso2709ReorderingDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset(),
                            new VolumeIncludeParents(jobEntity.getId(), entityManager));
                case DANMARC2_LINE_FORMAT:
                    return getDanMarc2LineFormatPartitioner();
                case DANMARC2_LINE_FORMAT_COLLECTION:
                    return DanMarc2LineFormatReorderingDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset(),
                            new VolumeIncludeParents(jobEntity.getId(), entityManager));
                case ADDI_MARC_XML:
                    return MarcXchangeAddiDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset());
                case ADDI:
                    return AddiDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset());
                case CSV:
                    return CsvDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset());
                case DSD_CSV:
                    return DsdCsvDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset());
                case JSON:
                    return JsonDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset());
                case VIP_CSV:
                    return VipCsvDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset());
                case VIAF:
                    return ViafDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset());
                case TARRED_XML:
                    return TarredXmlDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset());
                case ZIPPED_XML:
                    return ZippedXmlDataPartitioner.newInstance(
                            dataFileInputStream, jobEntity.getSpecification().getCharset());
                default:
                    diagnostics.add(ObjectFactory.buildFatalDiagnostic(
                            "unknown data partitioner: " + recordSplitterType));
            }
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

    private DataPartitioner getDanMarc2LineFormatPartitioner() {
        final String encoding = jobEntity.getSpecification().getCharset();
        switch (getTypeOfReordering()) {
            case VOLUME_INCLUDE_PARENTS:
                return DanMarc2LineFormatReorderingDataPartitioner.newInstance(
                        dataFileInputStream, encoding,
                        new VolumeIncludeParents(jobEntity.getId(), entityManager));
            case VOLUME_AFTER_PARENTS:
                return DanMarc2LineFormatReorderingDataPartitioner.newInstance(
                        dataFileInputStream, encoding,
                        new VolumeAfterParents(jobEntity.getId(), entityManager));
            default:
                return DanMarc2LineFormatDataPartitioner.newInstance(
                        dataFileInputStream, encoding);
        }
    }

    private DataPartitioner getIso2709Partitioner() {
        final String encoding = jobEntity.getSpecification().getCharset();
        switch (getTypeOfReordering()) {
            case VOLUME_INCLUDE_PARENTS:
                return Iso2709ReorderingDataPartitioner.newInstance(
                        dataFileInputStream, encoding,
                        new VolumeIncludeParents(jobEntity.getId(), entityManager));
            case VOLUME_AFTER_PARENTS:
                return Iso2709ReorderingDataPartitioner.newInstance(
                        dataFileInputStream, encoding,
                        new VolumeAfterParents(jobEntity.getId(), entityManager));
            default:
                return Iso2709DataPartitioner.newInstance(
                        dataFileInputStream, encoding);
        }
    }

    private enum TYPE_OF_REORDERING {
        VOLUME_AFTER_PARENTS,
        VOLUME_INCLUDE_PARENTS,
        NONE
    }

    private TYPE_OF_REORDERING getTypeOfReordering() {
        final JobSpecification.Ancestry ancestry =
                jobEntity.getSpecification().getAncestry();
        // Items originating from external sources must undergo potential re-ordering
        if (ancestry != null && ancestry.getTransfile() != null && !previewOnly) {
            return TYPE_OF_REORDERING.VOLUME_AFTER_PARENTS;
        }
        return TYPE_OF_REORDERING.NONE;
    }
}
