package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.sink.fbs.connector.FbsUpdateConnector;
import dk.dbc.dataio.sink.fbs.types.FbsUpdateConnectorException;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeStatusEnum;
import java.util.ArrayList;
import java.util.Iterator;
import org.junit.Test;

import javax.xml.ws.WebServiceException;

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
        final ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(new ArrayList<ChunkItem>()).build();
        processedChunk.insertItem(new ChunkItemBuilder().setId(0).setData(Base64Util.base64encode(inData0)).build());
        processedChunk.insertItem(new ChunkItemBuilder().setId(1).setData(Base64Util.base64encode(inData1)).build());
        processedChunk.insertItem(new ChunkItemBuilder().setId(2).setData(Base64Util.base64encode(inData2)).build());
        processedChunk.insertItem(new ChunkItemBuilder().setId(3).setData(Base64Util.base64encode(inData3)).build());
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

        final ExternalChunk deliveredChunk = fbsPusherBean.push(processedChunk);
        assertThat(deliveredChunk.getType(), is(ExternalChunk.Type.DELIVERED));
        assertThat(deliveredChunk.size(), is(6));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(item1.getData(), is(Base64Util.base64encode(okMessage)));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item3 = iterator.next();
        assertThat(item3.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(item3.getData(), is(Base64Util.base64encode(failedMessage)));
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
        fbsPusherBean.push(new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build());
    }

    @Test(expected = WebServiceException.class)
    public void push_serviceRespondsWithPleaseResendLater_throws() throws FbsUpdateConnectorException {
        final UpdateMarcXchangeResult updateMarcXchangeResultResendLater = new UpdateMarcXchangeResult();
        updateMarcXchangeResultResendLater.setUpdateMarcXchangeStatus(UpdateMarcXchangeStatusEnum.UPDATE_FAILED_PLEASE_RESEND_LATER);
        when(fbsUpdateConnectorBean.getConnector()).thenReturn(fbsUpdateConnector);
        when(fbsUpdateConnector.updateMarcExchange(anyString(), anyString())).thenReturn(updateMarcXchangeResultResendLater);
        fbsPusherBean.push(new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build());
    }

    private FbsPusherBean getInitializedBean() {
        final FbsPusherBean fbsPusherBean = new FbsPusherBean();
        fbsPusherBean.fbsUpdateConnector = fbsUpdateConnectorBean;
        return fbsPusherBean;
    }
}