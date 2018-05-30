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
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConversionFinalizerBeanIT extends IntegrationTest {
    private static final String FILE_STORE_URL = "http://filestore";
    private static final String FILE_ID = "123456789";

    private final FileStoreServiceConnectorBean fileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);

    @Before
    public void setupMocks() throws FileStoreServiceConnectorException {
        when(fileStoreServiceConnectorBean.getConnector()).thenReturn(fileStoreServiceConnector);
        when(fileStoreServiceConnector.addFile(any(InputStream.class))).thenReturn(FILE_ID);
        when(fileStoreServiceConnector.getBaseUrl()).thenReturn(FILE_STORE_URL);
    }

    @Test
    public void handleTerminationChunk() throws FileStoreServiceConnectorException {
        final ConversionBlock block0 = new ConversionBlock();
        block0.setKey(new ConversionBlock.Key(42, 0));
        block0.setBytes(StringUtil.asBytes("0"));
        final ConversionBlock block1 = new ConversionBlock();
        block1.setKey(new ConversionBlock.Key(42, 1));
        block1.setBytes(StringUtil.asBytes("1"));
        final ConversionBlock block2 = new ConversionBlock();
        block2.setKey(new ConversionBlock.Key(42, 2));
        block2.setBytes(StringUtil.asBytes("2"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        final ConversionFinalizerBean conversionFinalizerBean = newConversionFinalizerBean();
        final Chunk chunk = new Chunk(42, 3, Chunk.Type.DELIVERED);
        final Chunk result = env().getPersistenceContext().run(() ->
                conversionFinalizerBean.handleTerminationChunk(chunk));

        final InOrder orderVerifier = Mockito.inOrder(fileStoreServiceConnector);
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, block1.getBytes());
        orderVerifier.verify(fileStoreServiceConnector).appendToFile(FILE_ID, block2.getBytes());

        assertThat("result chunk size", result.getItems().size(),
                is(1));
        assertThat("result chunk job", result.getJobId(),
                is(42L));
        assertThat("result chunk id", result.getChunkId(),
                is(3L));
        assertThat("result chunk status", result.getItems().get(0).getStatus(),
                is(ChunkItem.Status.SUCCESS));
        assertThat("result chunk data", StringUtil.asString(result.getItems().get(0).getData()),
                is(String.join("/", FILE_STORE_URL, "files", FILE_ID)));
    }

    private ConversionFinalizerBean newConversionFinalizerBean() {
        final ConversionFinalizerBean conversionFinalizerBean = new ConversionFinalizerBean();
        conversionFinalizerBean.entityManager = env().getEntityManager();
        conversionFinalizerBean.fileStoreServiceConnectorBean = fileStoreServiceConnectorBean;
        return conversionFinalizerBean;
    }
}