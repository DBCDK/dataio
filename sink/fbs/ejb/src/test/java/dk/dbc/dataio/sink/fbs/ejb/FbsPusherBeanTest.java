package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
import dk.dbc.dataio.sink.fbs.connector.FbsUpdateConnector;
import dk.dbc.dataio.sink.fbs.types.FbsUpdateConnectorException;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeStatusEnum;
import org.junit.Test;

import java.util.Collections;

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
    public void push_multipleChunkItems_returnsSinkChunkResult() throws FbsUpdateConnectorException {
        final String inData0 = "zero";
        final String inData1 = "one";
        final String inData2 = "two";
        final String inData3 = "three";
        final ChunkResult chunkResult = new ChunkResultBuilder().setItems(Collections.<ChunkItem>emptyList()).build();
        chunkResult.addItem(new ChunkItemBuilder().setId(0).setData(Base64Util.base64encode(inData0)).build());
        chunkResult.addItem(new ChunkItemBuilder().setId(1).setData(Base64Util.base64encode(inData1)).build());
        chunkResult.addItem(new ChunkItemBuilder().setId(2).setData(Base64Util.base64encode(inData2)).build());
        chunkResult.addItem(new ChunkItemBuilder().setId(3).setData(Base64Util.base64encode(inData3)).build());
        chunkResult.addItem(new ChunkItemBuilder().setId(4).setStatus(ChunkItem.Status.FAILURE).build());
        chunkResult.addItem(new ChunkItemBuilder().setId(5).setStatus(ChunkItem.Status.IGNORE).build());

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

        final SinkChunkResult sinkChunkResult = fbsPusherBean.push(chunkResult);
        assertThat(sinkChunkResult.getItems().size(), is(6));
        assertThat(sinkChunkResult.getItems().get(0).getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(sinkChunkResult.getItems().get(1).getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(sinkChunkResult.getItems().get(1).getData(), is(Base64Util.base64encode(okMessage)));
        assertThat(sinkChunkResult.getItems().get(2).getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(sinkChunkResult.getItems().get(3).getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(sinkChunkResult.getItems().get(3).getData(), is(Base64Util.base64encode(failedMessage)));
        assertThat(sinkChunkResult.getItems().get(4).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(sinkChunkResult.getItems().get(5).getStatus(), is(ChunkItem.Status.IGNORE));
    }

    private FbsPusherBean getInitializedBean() {
        final FbsPusherBean fbsPusherBean = new FbsPusherBean();
        fbsPusherBean.fbsUpdateConnector = fbsUpdateConnectorBean;
        return fbsPusherBean;
    }
}