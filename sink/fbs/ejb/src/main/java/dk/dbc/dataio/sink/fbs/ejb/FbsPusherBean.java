package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeStatusEnum;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.ArrayList;

@Stateless
public class FbsPusherBean {
    @EJB
    FbsUpdateConnectorBean fbsUpdateConnector;

    public SinkChunkResult push(ChunkResult chunkResult) {
        final SinkChunkResult sinkChunkResult = new SinkChunkResult(chunkResult.getJobId(),
                chunkResult.getChunkId(), chunkResult.getEncoding(), new ArrayList<ChunkItem>());
        for (ChunkItem chunkItem : chunkResult.getItems()) {
            if (chunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
                executeUpdateOperation(sinkChunkResult, chunkItem);
            } else {
                sinkChunkResult.addItem(newIgnoredChunkItem(chunkItem.getId(),
                        String.format("Processor item status was: %s", chunkItem.getStatus())));
            }
        }
        return sinkChunkResult;
    }

    private void executeUpdateOperation(SinkChunkResult sinkChunkResult, ChunkItem chunkItem) {
        final String trackingId = String.format("%d-%d-%d", sinkChunkResult.getJobId(), sinkChunkResult.getChunkId(), chunkItem.getId());
        try {
            final UpdateMarcXchangeResult updateMarcXchangeResult = fbsUpdateConnector.updateMarcExchange(
                    Base64Util.base64decode(chunkItem.getData()), trackingId);
            if (updateMarcXchangeResult.getUpdateMarcXchangeStatus() == UpdateMarcXchangeStatusEnum.OK) {
                sinkChunkResult.addItem(newSuccessfulChunkItem(chunkItem.getId(), updateMarcXchangeResult.getUpdateMarcXchangeMessage()));
            } else {
                sinkChunkResult.addItem(newFailedChunkItem(chunkItem.getId(), updateMarcXchangeResult.getUpdateMarcXchangeMessage()));
            }
        } catch (Exception e) {
            sinkChunkResult.addItem(newFailedChunkItem(chunkItem.getId(), ServiceUtil.stackTraceToString(e)));
        }
    }

    private ChunkItem newSuccessfulChunkItem(long chunkItemId, String data) {
        return newChunkItem(chunkItemId, data, ChunkItem.Status.SUCCESS);
    }

    private ChunkItem newFailedChunkItem(long chunkItemId, String data) {
        return newChunkItem(chunkItemId, data, ChunkItem.Status.FAILURE);
    }

    private ChunkItem newIgnoredChunkItem(long chunkItemId, String data) {
        return newChunkItem(chunkItemId, data, ChunkItem.Status.IGNORE);
    }

    private ChunkItem newChunkItem(long chunkItemId, String data, ChunkItem.Status status) {
        if (data != null && !data.isEmpty()) {
            data = Base64Util.base64encode(data);
        } else {
            data = "";
        }
        return new ChunkItem(chunkItemId, data, status);
    }
}
