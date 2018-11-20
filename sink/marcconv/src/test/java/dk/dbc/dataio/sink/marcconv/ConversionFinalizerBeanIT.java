/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.sink.marcconv;

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
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import javax.persistence.PersistenceException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConversionFinalizerBeanIT extends IntegrationTest {
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
                any(ConversionMetadata.class), eq(ConversionFinalizerBean.ExistingFile.class)))
                .thenReturn(Collections.emptyList());
    }

    @Test
    public void handleTerminationChunk() throws FileStoreServiceConnectorException {
        final ConversionBlock block0 = new ConversionBlock();
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0));
        block0.setBytes(StringUtil.asBytes("0"));
        final ConversionBlock block1 = new ConversionBlock();
        block1.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 1));
        block1.setBytes(StringUtil.asBytes("1"));
        final ConversionBlock block2 = new ConversionBlock();
        block2.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 2));
        block2.setBytes(StringUtil.asBytes("2"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final ConversionFinalizerBean conversionFinalizerBean = newConversionFinalizerBean();
        final Chunk chunk = new Chunk(jobInfoSnapshot.getJobId(), 3, Chunk.Type.DELIVERED);
        final Chunk result = env().getPersistenceContext().run(() ->
                conversionFinalizerBean.handleTerminationChunk(chunk));

        final InOrder orderVerifier = Mockito.inOrder(fileStoreServiceConnector);
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, block1.getBytes());
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, block2.getBytes());

        final ConversionMetadata expectedMetadata = new ConversionMetadata()
                .withJobId(jobInfoSnapshot.getJobId())
                .withAgencyId((int) jobInfoSnapshot.getSpecification().getSubmitterId())
                .withFilename(jobInfoSnapshot.getSpecification().getAncestry().getDatafile());
        verify(fileStoreServiceConnector).addMetadata(FILE_ID, expectedMetadata);

        assertThat("result chunk size", result.getItems().size(),
                is(1));
        assertThat("result chunk job", result.getJobId(),
                is((long) jobInfoSnapshot.getJobId()));
        assertThat("result chunk id", result.getChunkId(),
                is(3L));
        assertThat("result chunk status", result.getItems().get(0).getStatus(),
                is(ChunkItem.Status.SUCCESS));
        assertThat("result chunk data", StringUtil.asString(result.getItems().get(0).getData()),
                is(String.join("/", FILE_STORE_URL, "files", FILE_ID)));

        final List<ConversionBlock> blocks = env().getEntityManager()
                .createNamedQuery(ConversionBlock.GET_CONVERSION_BLOCKS_QUERY_NAME,
                        ConversionBlock.class)
                .setParameter(1, jobInfoSnapshot.getJobId())
                .getResultList();
        assertThat("blocks deleted", blocks.isEmpty(), is(true));
    }

    @Test
    public void conversionParamOverrideAgencyId() throws FileStoreServiceConnectorException {
        final ConversionBlock block0 = new ConversionBlock();
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0));
        block0.setBytes(StringUtil.asBytes("0"));

        final ConversionParam param = new ConversionParam()
                .withSubmitter(123789);
        final StoredConversionParam scp = new StoredConversionParam(jobInfoSnapshot.getJobId());
        scp.setParam(param);

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block0);
            env().getEntityManager().persist(scp);
        });

        final ConversionFinalizerBean conversionFinalizerBean = newConversionFinalizerBean();
        final Chunk chunk = new Chunk(jobInfoSnapshot.getJobId(), 0, Chunk.Type.DELIVERED);
        env().getPersistenceContext().run(() -> conversionFinalizerBean.handleTerminationChunk(chunk));

        final StoredConversionParam storedConversionParam = env().getPersistenceContext().run(() ->
            env().getEntityManager().find(StoredConversionParam.class, Math.toIntExact(chunk.getJobId())));
        assertThat("StoredConversionParam", storedConversionParam, is(nullValue()));

        final ConversionMetadata expectedMetadata = new ConversionMetadata()
                .withJobId(jobInfoSnapshot.getJobId())
                .withAgencyId(123789)
                .withFilename(jobInfoSnapshot.getSpecification().getAncestry().getDatafile());
        verify(fileStoreServiceConnector).addMetadata(FILE_ID, expectedMetadata);
    }

    @Test
    public void fileAlreadyExist() throws FileStoreServiceConnectorException {
        final ConversionMetadata metadata = new ConversionMetadata()
                .withJobId(jobInfoSnapshot.getJobId());
        when(fileStoreServiceConnector.searchByMetadata(
                metadata, ConversionFinalizerBean.ExistingFile.class))
                .thenReturn(Collections.singletonList(
                        new ConversionFinalizerBean.ExistingFile(FILE_ID)));

        final ConversionFinalizerBean conversionFinalizerBean = newConversionFinalizerBean();
        final Chunk chunk = new Chunk(jobInfoSnapshot.getJobId(), 3, Chunk.Type.DELIVERED);
        env().getPersistenceContext().run(() ->
                conversionFinalizerBean.handleTerminationChunk(chunk));

        // verify no uploading to file-store
        verify(fileStoreServiceConnector, times(0)).addFile(any());
        verify(fileStoreServiceConnector, times(0)).appendToFile(any(), any());
        verify(fileStoreServiceConnector, times(0)).addMetadata(any(), any());
    }

    @Test
    public void exceptionFromFileUpload() throws FileStoreServiceConnectorException, SinkException {
        when(fileStoreServiceConnector.addFile(any(InputStream.class)))
                .thenThrow(new PersistenceException("died"));

        final ConversionBlock block0 = new ConversionBlock();
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0));
        block0.setBytes(StringUtil.asBytes("0"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block0);
        });

        final ConversionFinalizerBean conversionFinalizerBean = newConversionFinalizerBean();
        final Chunk chunk = new Chunk(jobInfoSnapshot.getJobId(), 3, Chunk.Type.DELIVERED);
        try {
            env().getPersistenceContext().run(() ->
                conversionFinalizerBean.handleTerminationChunk(chunk));
            fail("no RuntimeException thrown");
        } catch (RuntimeException e) {
            assertThat("SinkException thrown",
                    e.getCause() instanceof SinkException, is(true));
        }

        verify(fileStoreServiceConnector).deleteFile((String) null);
    }

    @Test
    public void exceptionFromMetadataUpload() throws FileStoreServiceConnectorException, SinkException {
        doThrow(new PersistenceException("died"))
                .when(fileStoreServiceConnector).addMetadata(any(), any());

        final ConversionBlock block0 = new ConversionBlock();
        block0.setKey(new ConversionBlock.Key(jobInfoSnapshot.getJobId(), 0));
        block0.setBytes(StringUtil.asBytes("0"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block0);
        });

        final ConversionFinalizerBean conversionFinalizerBean = newConversionFinalizerBean();
        final Chunk chunk = new Chunk(jobInfoSnapshot.getJobId(), 3, Chunk.Type.DELIVERED);
        try {
            env().getPersistenceContext().run(() ->
                    conversionFinalizerBean.handleTerminationChunk(chunk));
            fail("no RuntimeException thrown");
        } catch (RuntimeException e) {
            assertThat("SinkException thrown",
                    e.getCause() instanceof SinkException, is(true));
        }

        verify(fileStoreServiceConnector).deleteFile(FILE_ID);
    }

    private ConversionFinalizerBean newConversionFinalizerBean() {
        final ConversionFinalizerBean conversionFinalizerBean = new ConversionFinalizerBean();
        conversionFinalizerBean.entityManager = env().getEntityManager();
        conversionFinalizerBean.fileStoreServiceConnectorBean = fileStoreServiceConnectorBean;
        conversionFinalizerBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return conversionFinalizerBean;
    }
}