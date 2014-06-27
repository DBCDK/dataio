package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.sink.fbs.connector.FbsUpdateConnector;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.ArrayList;

@Stateless
public class FbsPusherBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FbsPusherBean.class);

    @EJB
    FbsUpdateConnectorBean fbsUpdateConnector;

    public SinkChunkResult push(ChunkResult chunkResult) {
        LOGGER.info("Examining ChunkResult {} for job {}", chunkResult.getChunkId(), chunkResult.getJobId());
        final SinkChunkResult sinkChunkResult = new SinkChunkResult(chunkResult.getJobId(),
                chunkResult.getChunkId(), chunkResult.getEncoding(), new ArrayList<ChunkItem>());

        int itemsPushed = 0;
        for (ChunkItem chunkItem : chunkResult.getItems()) {
            if (chunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
                executeUpdateOperation(sinkChunkResult, chunkItem);
                itemsPushed++;
            } else {
                sinkChunkResult.addItem(newIgnoredChunkItem(chunkItem.getId(),
                        String.format("Processor item status was: %s", chunkItem.getStatus())));
            }
        }
        LOGGER.info("Pushed {} items from ChunkResult {} for job {}",
                itemsPushed, chunkResult.getChunkId(), chunkResult.getJobId());

        return sinkChunkResult;
    }

    private void executeUpdateOperation(SinkChunkResult sinkChunkResult, ChunkItem chunkItem) {
        final String trackingId = String.format("%d-%d-%d", sinkChunkResult.getJobId(), sinkChunkResult.getChunkId(), chunkItem.getId());
        final FbsUpdateConnector connector = fbsUpdateConnector.getConnector();
        try {
            final UpdateMarcXchangeResult updateMarcXchangeResult = connector.updateMarcExchange(
                    Base64Util.base64decode(chunkItem.getData()), trackingId);
            if (updateMarcXchangeResult.getUpdateMarcXchangeStatus() == UpdateMarcXchangeStatusEnum.OK) {
                sinkChunkResult.addItem(newSuccessfulChunkItem(chunkItem.getId(), updateMarcXchangeResult.getUpdateMarcXchangeMessage()));
            } else {
                sinkChunkResult.addItem(newFailedChunkItem(chunkItem.getId(), updateMarcXchangeResult.getUpdateMarcXchangeMessage()));
            }
        } catch (Exception e) {
            LOGGER.error("Item {} registered as FAILED for ChunkResult {} for job {} due to exception",
                    chunkItem.getId(), sinkChunkResult.getChunkId(), sinkChunkResult.getJobId(), e);
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
