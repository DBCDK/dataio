package dk.dbc.dataio.sink.marcconv.entity;

import dk.dbc.dataio.commons.conversion.ConversionMetadata;
import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.marcconv.IntegrationTest;
import jakarta.persistence.PersistenceException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConversionFinalizerIT extends IntegrationTest {
    private static final String FILE_STORE_URL = "http://filestore";
    private static final String FILE_ID = "123456789";

    private final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
            .withJobId(42)
            .withSpecification(
                    new JobSpecification()
                            .withSubmitterId(870970)
                            .withAncestry(new JobSpecification.Ancestry()
                                    .withDatafile("test.iso")));

    private final FileStoreServiceConnectorBean fileStoreServiceConnectorBean =
            mock(FileStoreServiceConnectorBean.class);
    private final FileStoreServiceConnector fileStoreServiceConnector =
            mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean =
            mock(JobStoreServiceConnectorBean.class);
    private final JobStoreServiceConnector jobStoreServiceConnector =
            mock(JobStoreServiceConnector.class);

    @Before
    public void setupMocks() throws FileStoreServiceConnectorException, JobStoreServiceConnectorException {
        when(fileStoreServiceConnectorBean.getConnector())
                .thenReturn(fileStoreServiceConnector);
        when(fileStoreServiceConnector.addFile(any(InputStream.class)))
                .thenReturn(FILE_ID);
        when(fileStoreServiceConnector.getBaseUrl())
                .thenReturn(FILE_STORE_URL);
        when(jobStoreServiceConnectorBean.getConnector())
                .thenReturn(jobStoreServiceConnector);
        when(jobStoreServiceConnector.listJobs(new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_ID,
                        ListFilter.Op.EQUAL, jobInfoSnapshot.getJobId()))))
                .thenReturn(Collections.singletonList(jobInfoSnapshot));
        when(fileStoreServiceConnector.searchByMetadata(
                any(ConversionMetadata.class), eq(ConversionFinalizer.ExistingFile.class)))
                .thenReturn(Collections.emptyList());
    }

    @Test
    public void handleTerminationChunk() throws FileStoreServiceConnectorException {
        ConversionBlock block0 = new ConversionBlock();
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0));
        block0.setBytes(StringUtil.asBytes("0"));
        ConversionBlock block1 = new ConversionBlock();
        block1.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 1));
        block1.setBytes(StringUtil.asBytes("1"));
        ConversionBlock block2 = new ConversionBlock();
        block2.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 2));
        block2.setBytes(StringUtil.asBytes("2"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        ConversionFinalizer conversionFinalizer = newConversionFinalizerBean();
        Chunk chunk = new Chunk(jobInfoSnapshot.getJobId(), 3, Chunk.Type.DELIVERED);
        Chunk result = env().getPersistenceContext().run(() ->
                conversionFinalizer.handleTerminationChunk(chunk));

        InOrder orderVerifier = Mockito.inOrder(fileStoreServiceConnector);
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, block1.getBytes());
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, block2.getBytes());

        ConversionMetadata expectedMetadata = new ConversionMetadata(ConversionFinalizer.ORIGIN)
                .withJobId(jobInfoSnapshot.getJobId())
                .withAgencyId((int) jobInfoSnapshot.getSpecification().getSubmitterId())
                .withFilename(jobInfoSnapshot.getSpecification().getAncestry().getDatafile());
        verify(fileStoreServiceConnector).addMetadata(FILE_ID, expectedMetadata);

        assertThat("result chunk size", result.getItems().size(),
                is(1));
        assertThat("result chunk job", result.getJobId(),
                is(jobInfoSnapshot.getJobId()));
        assertThat("result chunk id", result.getChunkId(),
                is(3L));
        assertThat("result chunk status", result.getItems().get(0).getStatus(),
                is(ChunkItem.Status.SUCCESS));
        assertThat("result chunk data", StringUtil.asString(result.getItems().get(0).getData()),
                is(String.join("/", FILE_STORE_URL, "files", FILE_ID)));

        List<ConversionBlock> blocks = env().getEntityManager()
                .createNamedQuery(ConversionBlock.GET_CONVERSION_BLOCKS_QUERY_NAME,
                        ConversionBlock.class)
                .setParameter(1, jobInfoSnapshot.getJobId())
                .getResultList();
        assertThat("blocks deleted", blocks.isEmpty(), is(true));
    }

    @Test
    public void conversionParamOverrideAgencyId() throws FileStoreServiceConnectorException {
        ConversionBlock block0 = new ConversionBlock();
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0));
        block0.setBytes(StringUtil.asBytes("0"));

        ConversionParam param = new ConversionParam()
                .withSubmitter(123789);
        StoredConversionParam scp = new StoredConversionParam(jobInfoSnapshot.getJobId());
        scp.setParam(param);

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block0);
            env().getEntityManager().persist(scp);
        });

        ConversionFinalizer conversionFinalizer = newConversionFinalizerBean();
        Chunk chunk = new Chunk(jobInfoSnapshot.getJobId(), 0, Chunk.Type.DELIVERED);
        env().getPersistenceContext().run(() -> conversionFinalizer.handleTerminationChunk(chunk));

        StoredConversionParam storedConversionParam = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(StoredConversionParam.class, Math.toIntExact(chunk.getJobId())));
        assertThat("StoredConversionParam", storedConversionParam, is(nullValue()));

        ConversionMetadata expectedMetadata = new ConversionMetadata(ConversionFinalizer.ORIGIN)
                .withJobId(jobInfoSnapshot.getJobId())
                .withAgencyId(123789)
                .withFilename(jobInfoSnapshot.getSpecification().getAncestry().getDatafile());
        verify(fileStoreServiceConnector).addMetadata(FILE_ID, expectedMetadata);
    }

    @Test
    public void fileAlreadyExist() throws FileStoreServiceConnectorException {
        ConversionMetadata metadata = new ConversionMetadata(ConversionFinalizer.ORIGIN)
                .withJobId(jobInfoSnapshot.getJobId())
                .withAgencyId(870970)
                .withFilename("test.iso");
        when(fileStoreServiceConnector.searchByMetadata(
                metadata, ConversionFinalizer.ExistingFile.class))
                .thenReturn(Collections.singletonList(
                        new ConversionFinalizer.ExistingFile(FILE_ID)));

        ConversionFinalizer conversionFinalizer = newConversionFinalizerBean();
        Chunk chunk = new Chunk(jobInfoSnapshot.getJobId(), 3, Chunk.Type.DELIVERED);
        env().getPersistenceContext().run(() ->
                conversionFinalizer.handleTerminationChunk(chunk));

        // verify no uploading to file-store
        verify(fileStoreServiceConnector, times(0)).addFile(any());
        verify(fileStoreServiceConnector, times(0)).appendToFile(any(), any());
        verify(fileStoreServiceConnector, times(0)).addMetadata(any(), any());
    }

    @Test
    public void exceptionFromFileUpload() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnector.addFile(any(InputStream.class)))
                .thenThrow(new PersistenceException("died"));

        ConversionBlock block0 = new ConversionBlock();
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0));
        block0.setBytes(StringUtil.asBytes("0"));

        env().getPersistenceContext().run(() -> env().getEntityManager().persist(block0));

        ConversionFinalizer conversionFinalizer = newConversionFinalizerBean();
        Chunk chunk = new Chunk(jobInfoSnapshot.getJobId(), 3, Chunk.Type.DELIVERED);
        try {
            env().getPersistenceContext().run(() ->
                    conversionFinalizer.handleTerminationChunk(chunk));
            fail("no RuntimeException thrown");
        } catch (RuntimeException ignored) {
        }

        verify(fileStoreServiceConnector).deleteFile((String) null);
    }

    @Test
    public void exceptionFromMetadataUpload() throws FileStoreServiceConnectorException {
        doThrow(new PersistenceException("died"))
                .when(fileStoreServiceConnector).addMetadata(any(), any());

        ConversionBlock block0 = new ConversionBlock();
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0));
        block0.setBytes(StringUtil.asBytes("0"));

        env().getPersistenceContext().run(() -> env().getEntityManager().persist(block0));

        ConversionFinalizer conversionFinalizer = newConversionFinalizerBean();
        Chunk chunk = new Chunk(jobInfoSnapshot.getJobId(), 3, Chunk.Type.DELIVERED);
        try {
            env().getPersistenceContext().run(() ->
                    conversionFinalizer.handleTerminationChunk(chunk));
            fail("no RuntimeException thrown");
        } catch (RuntimeException ignored) {
        }

        verify(fileStoreServiceConnector).deleteFile(FILE_ID);
    }

    private ConversionFinalizer newConversionFinalizerBean() {
        ServiceHub hub = new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).build();
        return new ConversionFinalizer(hub, fileStoreServiceConnector, env().getEntityManager());
    }
}
