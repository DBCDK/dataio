package dk.dbc.dataio.sink.marcconv.entity;

import dk.dbc.dataio.commons.conversion.ConversionMetadata;
import dk.dbc.dataio.commons.conversion.ConversionParam;
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
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
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

    private final ChunkItem terminationItem = new ChunkItem()
            .withId(7)
            .withTrackingId("tracked")
            .withType(ChunkItem.Type.JOB_END)
            .withStatus(ChunkItem.Status.SUCCESS)
            .withData("termination");

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
    public void finalizeJob() throws FileStoreServiceConnectorException {
        persistBlocks(
                newBlock(0, 0, "a"),
                newBlock(0, 1, "b"),
                newBlock(1, 0, "c"));

        ConversionFinalizer conversionFinalizer = newConversionFinalizerBean();
        ChunkItem result = env().getPersistenceContext().run(() ->
                conversionFinalizer.finalizeJob(jobInfoSnapshot.getJobId(), terminationItem, env().getEntityManager()));

        // The blocks of a job are uploaded as one file, in ascending chunk and item order,
        // and buffered into a single call since they are well below the buffer size.
        verify(fileStoreServiceConnector).addFile(any(InputStream.class));
        verify(fileStoreServiceConnector, times(0)).appendToFile(any(), any());
        assertThat("uploaded bytes", uploadedBytes(), is("abc"));

        ConversionMetadata expectedMetadata = new ConversionMetadata(ConversionFinalizer.ORIGIN)
                .withJobId(jobInfoSnapshot.getJobId())
                .withAgencyId((int) jobInfoSnapshot.getSpecification().getSubmitterId())
                .withFilename(jobInfoSnapshot.getSpecification().getAncestry().getDatafile());
        verify(fileStoreServiceConnector).addMetadata(FILE_ID, expectedMetadata);

        assertThat("result status", result.getStatus(),
                is(ChunkItem.Status.SUCCESS));
        assertThat("result type", result.getType().getFirst(),
                is(ChunkItem.Type.JOB_END));
        assertThat("result id", result.getId(),
                is(terminationItem.getId()));
        assertThat("result tracking id", result.getTrackingId(),
                is(terminationItem.getTrackingId()));
        assertThat("result data", StringUtil.asString(result.getData()),
                is(String.join("/", FILE_STORE_URL, "files", FILE_ID)));

        List<ConversionBlock> blocks = env().getEntityManager()
                .createNamedQuery(ConversionBlock.GET_CONVERSION_BLOCKS_QUERY_NAME,
                        ConversionBlock.class)
                .setParameter(1, jobInfoSnapshot.getJobId())
                .getResultList();
        assertThat("blocks deleted", blocks.isEmpty(), is(true));
    }

    @Test
    public void blocksBeyondTheBufferSizeAreAppended() throws FileStoreServiceConnectorException {
        byte[] firstHalf = filler('x');
        byte[] secondHalf = filler('y');
        ConversionBlock block0 = new ConversionBlock();
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0, 0));
        block0.setBytes(firstHalf);
        ConversionBlock block1 = new ConversionBlock();
        block1.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0, 1));
        block1.setBytes(secondHalf);
        persistBlocks(block0, block1);

        ConversionFinalizer conversionFinalizer = newConversionFinalizerBean();
        env().getPersistenceContext().run(() ->
                conversionFinalizer.finalizeJob(jobInfoSnapshot.getJobId(), terminationItem, env().getEntityManager()));

        verify(fileStoreServiceConnector).addFile(any(InputStream.class));
        verify(fileStoreServiceConnector).appendToFile(eq(FILE_ID), any(byte[].class));
    }

    @Test
    public void jobWithoutConversionOutputIsFailed() throws FileStoreServiceConnectorException {
        ConversionFinalizer conversionFinalizer = newConversionFinalizerBean();
        ChunkItem result = env().getPersistenceContext().run(() ->
                conversionFinalizer.finalizeJob(jobInfoSnapshot.getJobId(), terminationItem, env().getEntityManager()));

        verify(fileStoreServiceConnector, times(0)).addFile(any());
        verify(fileStoreServiceConnector, times(0)).addMetadata(any(), any());
        assertThat("result status", result.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("result type", result.getType().getFirst(), is(ChunkItem.Type.JOB_END));
        assertThat("result diagnostics", result.getDiagnostics(), is(notNullValue()));
    }

    @Test
    public void conversionParamOverrideAgencyId() throws FileStoreServiceConnectorException {
        ConversionBlock block0 = new ConversionBlock();
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0, 0));
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
        env().getPersistenceContext().run(() ->
                conversionFinalizer.finalizeJob(jobInfoSnapshot.getJobId(), terminationItem, env().getEntityManager()));

        StoredConversionParam storedConversionParam = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(StoredConversionParam.class, jobInfoSnapshot.getJobId()));
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
        env().getPersistenceContext().run(() ->
                conversionFinalizer.finalizeJob(jobInfoSnapshot.getJobId(), terminationItem, env().getEntityManager()));

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
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0, 0));
        block0.setBytes(StringUtil.asBytes("0"));

        env().getPersistenceContext().run(() -> env().getEntityManager().persist(block0));

        ConversionFinalizer conversionFinalizer = newConversionFinalizerBean();
        try {
            env().getPersistenceContext().run(() ->
                    conversionFinalizer.finalizeJob(jobInfoSnapshot.getJobId(), terminationItem, env().getEntityManager()));
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
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0, 0));
        block0.setBytes(StringUtil.asBytes("0"));

        env().getPersistenceContext().run(() -> env().getEntityManager().persist(block0));

        ConversionFinalizer conversionFinalizer = newConversionFinalizerBean();
        try {
            env().getPersistenceContext().run(() ->
                    conversionFinalizer.finalizeJob(jobInfoSnapshot.getJobId(), terminationItem, env().getEntityManager()));
            fail("no RuntimeException thrown");
        } catch (RuntimeException ignored) {
        }

        verify(fileStoreServiceConnector).deleteFile(FILE_ID);
    }

    private ConversionBlock newBlock(int chunkId, int itemId, String bytes) {
        ConversionBlock block = new ConversionBlock();
        block.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), chunkId, itemId));
        block.setBytes(StringUtil.asBytes(bytes));
        return block;
    }

    /**
     * Persists the blocks in reverse order, so that a test asserting on the order they are
     * uploaded in cannot be passed by the order they were written in
     */
    private void persistBlocks(ConversionBlock... blocks) {
        env().getPersistenceContext().run(() -> {
            for (int i = blocks.length - 1; i >= 0; i--) {
                env().getEntityManager().persist(blocks[i]);
            }
        });
    }

    private byte[] filler(char c) {
        byte[] bytes = new byte[ConversionFinalizer.UPLOAD_BUFFER_SIZE];
        Arrays.fill(bytes, (byte) c);
        return bytes;
    }

    private String uploadedBytes() throws FileStoreServiceConnectorException {
        ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);
        verify(fileStoreServiceConnector).addFile(captor.capture());
        try {
            return new String(captor.getValue().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private ConversionFinalizer newConversionFinalizerBean() {
        ServiceHub hub = new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).build();
        return new ConversionFinalizer(hub, fileStoreServiceConnector);
    }
}
