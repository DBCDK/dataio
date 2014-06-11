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
    // ToDo: how do we handle non-870970 records
    private final static String agencyId = "870970";

    @EJB
    FbsUpdateConnectorBean fbsUpdateConnector;

    public SinkChunkResult push(ChunkResult chunkResult) {
        final SinkChunkResult sinkChunkResult = new SinkChunkResult(chunkResult.getJobId(),
                chunkResult.getChunkId(), chunkResult.getEncoding(), new ArrayList<ChunkItem>());
        for (ChunkItem chunkItem : chunkResult.getItems()) {
            final String trackingId = String.format("%d-%d-%d", chunkResult.getJobId(), chunkResult.getChunkId(), chunkItem.getId());
            try {
                final UpdateMarcXchangeResult updateMarcXchangeResult = fbsUpdateConnector.updateMarcExchange(
                        agencyId, Base64Util.base64decode(chunkItem.getData()), trackingId);
                if (updateMarcXchangeResult.getUpdateMarcXchangeStatus() == UpdateMarcXchangeStatusEnum.OK) {
                    sinkChunkResult.addItem(newSuccessfulChunkItem(chunkItem.getId(), updateMarcXchangeResult.getUpdateMarcXchangeMessage()));
                } else {
                    sinkChunkResult.addItem(newFailedChunkItem(chunkItem.getId(), updateMarcXchangeResult.getUpdateMarcXchangeMessage()));
                }
            } catch (Exception e) {
                sinkChunkResult.addItem(newFailedChunkItem(chunkItem.getId(), ServiceUtil.stackTraceToString(e)));
            }
        }
        return sinkChunkResult;
    }

    private ChunkItem newSuccessfulChunkItem(long chunkItemId, String data) {
        return newChunkItem(chunkItemId, data, ChunkItem.Status.SUCCESS);
    }

    private ChunkItem newFailedChunkItem(long chunkItemId, String data) {
        return newChunkItem(chunkItemId, data, ChunkItem.Status.FAILURE);
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
