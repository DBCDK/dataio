/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.sink.fbs.connector.FbsUpdateConnector;
import dk.dbc.dataio.sink.fbs.types.FbsUpdateConnectorException;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeStatusEnum;
import org.junit.Test;

import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FbsPusherBeanTest {
    private final FbsUpdateConnectorBean fbsUpdateConnectorBean = mock(FbsUpdateConnectorBean.class);
    private final FbsPusherBean fbsPusherBean = getInitializedBean();
    private final FbsUpdateConnector fbsUpdateConnector = mock(FbsUpdateConnector.class);

    @Test(expected = NullPointerException.class)
    public void push_chunkResultArgIsNull_throws() {
        fbsPusherBean.push(null);
    }

    @Test
    public void push_multipleChunkItems_returnsDeliveredChunk() throws FbsUpdateConnectorException {
        final String inData0 = "zero";
        final String inData1 = "one";
        final String inData2 = "two";
        final String inData3 = "three";
        final Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(new ArrayList<ChunkItem>()).build();
        processedChunk.insertItem(new ChunkItemBuilder().setId(0).setData(StringUtil.asBytes(inData0)).build());
        processedChunk.insertItem(new ChunkItemBuilder().setId(1).setData(StringUtil.asBytes(inData1)).build());
        processedChunk.insertItem(new ChunkItemBuilder().setId(2).setData(StringUtil.asBytes(inData2)).build());
        processedChunk.insertItem(new ChunkItemBuilder().setId(3).setData(StringUtil.asBytes(inData3)).build());
        processedChunk.insertItem(new ChunkItemBuilder().setId(4).setStatus(ChunkItem.Status.FAILURE).build());
        processedChunk.insertItem(new ChunkItemBuilder().setId(5).setStatus(ChunkItem.Status.IGNORE).build());

        final UpdateMarcXchangeResult updateMarcXchangeResultOk = new UpdateMarcXchangeResult();
        updateMarcXchangeResultOk.setUpdateMarcXchangeStatus(UpdateMarcXchangeStatusEnum.OK);
        final String okMessage = "OK";
        updateMarcXchangeResultOk.setUpdateMarcXchangeMessage(okMessage);

        final UpdateMarcXchangeResult updateMarcXchangeResultFailed = new UpdateMarcXchangeResult();
        updateMarcXchangeResultFailed.setUpdateMarcXchangeStatus(UpdateMarcXchangeStatusEnum.UPDATE_FAILED_FATAL_INTERNAL_ERROR);
        final String failedMessage = "FAILED";
        updateMarcXchangeResultFailed.setUpdateMarcXchangeMessage(failedMessage);

        when(fbsUpdateConnectorBean.getConnector()).thenReturn(fbsUpdateConnector);
        when(fbsUpdateConnector.updateMarcExchange(eq(inData0), anyString())).thenThrow(new FbsUpdateConnectorException("DIED"));
        when(fbsUpdateConnector.updateMarcExchange(eq(inData1), anyString())).thenReturn(updateMarcXchangeResultOk);
        when(fbsUpdateConnector.updateMarcExchange(eq(inData2), anyString())).thenThrow(new IllegalStateException());
        when(fbsUpdateConnector.updateMarcExchange(eq(inData3), anyString())).thenReturn(updateMarcXchangeResultFailed);

        final Chunk deliveredChunk = fbsPusherBean.push(processedChunk);
        assertThat(deliveredChunk.getType(), is(Chunk.Type.DELIVERED));
        assertThat(deliveredChunk.size(), is(6));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(StringUtil.asString(item1.getData()), is(okMessage));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item3 = iterator.next();
        assertThat(item3.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(StringUtil.asString(item3.getData()), is(failedMessage));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item4 = iterator.next();
        assertThat(item4.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item5 = iterator.next();
        assertThat(item5.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = WebServiceException.class)
    public void push_connectorThrowsWebServiceException_throws() throws FbsUpdateConnectorException {
        when(fbsUpdateConnectorBean.getConnector()).thenReturn(fbsUpdateConnector);
        when(fbsUpdateConnector.updateMarcExchange(anyString(), anyString())).thenThrow(new WebServiceException("died"));
        fbsPusherBean.push(new ChunkBuilder(Chunk.Type.PROCESSED).build());
    }

    @Test(expected = WebServiceException.class)
    public void push_serviceRespondsWithPleaseResendLater_throws() throws FbsUpdateConnectorException {
        final UpdateMarcXchangeResult updateMarcXchangeResultResendLater = new UpdateMarcXchangeResult();
        updateMarcXchangeResultResendLater.setUpdateMarcXchangeStatus(UpdateMarcXchangeStatusEnum.UPDATE_FAILED_PLEASE_RESEND_LATER);
        when(fbsUpdateConnectorBean.getConnector()).thenReturn(fbsUpdateConnector);
        when(fbsUpdateConnector.updateMarcExchange(anyString(), anyString())).thenReturn(updateMarcXchangeResultResendLater);
        fbsPusherBean.push(new ChunkBuilder(Chunk.Type.PROCESSED).build());
    }

    private FbsPusherBean getInitializedBean() {
        final FbsPusherBean fbsPusherBean = new FbsPusherBean();
        fbsPusherBean.fbsUpdateConnector = fbsUpdateConnectorBean;
        return fbsPusherBean;
    }
}