package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.sink.fbs.connector.FbsUpdateConnector;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;

@Stateless
public class FbsPusherBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FbsPusherBean.class);

    @EJB
    FbsUpdateConnectorBean fbsUpdateConnector;

    /**
     * Pushes items of given ChunkResult to FBS web-service one ChunkItem at a
     * time
     * @param chunkResult chunk result ready for delivery
     * @return sink chunk result
     * @throws WebServiceException if service communication throws
     * WebServiceException or if service responds with
     * UPDATE_FAILED_PLEASE_RESEND_LATER status.
     */
    public SinkChunkResult push(ChunkResult chunkResult) throws WebServiceException {
        final StopWatch stopWatch = new StopWatch();
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
        LOGGER.info("Pushed {} items from ChunkResult {} for job {} in {} ms",
                itemsPushed, chunkResult.getChunkId(), chunkResult.getJobId(), stopWatch.getElapsedTime());

        return sinkChunkResult;
    }

    private void executeUpdateOperation(SinkChunkResult sinkChunkResult, ChunkItem chunkItem) throws WebServiceException {
        final String trackingId = String.format("%d-%d-%d", sinkChunkResult.getJobId(), sinkChunkResult.getChunkId(), chunkItem.getId());
        final FbsUpdateConnector connector = fbsUpdateConnector.getConnector();
        try {
            final UpdateMarcXchangeResult updateMarcXchangeResult = connector.updateMarcExchange(
                    Base64Util.base64decode(chunkItem.getData()), trackingId);
            switch(updateMarcXchangeResult.getUpdateMarcXchangeStatus()) {
                case OK:
                    sinkChunkResult.addItem(newSuccessfulChunkItem(
                            chunkItem.getId(), updateMarcXchangeResult.getUpdateMarcXchangeMessage()));
                    break;
                case UPDATE_FAILED_PLEASE_RESEND_LATER:
                    throw new WebServiceException("Service responded with 'please resend later' message");
                default:
                    sinkChunkResult.addItem(newFailedChunkItem(
                            chunkItem.getId(), updateMarcXchangeResult.getUpdateMarcXchangeMessage()));
            }
        } catch (WebServiceException e) {
            LOGGER.error("WebServiceException caught when handling Item {} for ChunkResult {} for job {}",
                    chunkItem.getId(), sinkChunkResult.getChunkId(), sinkChunkResult.getJobId(), e);
            throw e;
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
