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

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.sink.fbs.connector.FbsUpdateConnector;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.xml.ws.WebServiceException;

@Stateless
public class FbsPusherBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FbsPusherBean.class);

    @EJB
    FbsUpdateConnectorBean fbsUpdateConnector;

    /**
     * Pushes items of given chunk to FBS web-service one ChunkItem at a
     * time
     * @param processedChunk chunk result ready for delivery
     * @return sink chunk result
     * @throws WebServiceException if service communication throws
     * WebServiceException or if service responds with
     * UPDATE_FAILED_PLEASE_RESEND_LATER status.
     */
    public ExternalChunk push(ExternalChunk processedChunk) throws WebServiceException {
        final StopWatch stopWatch = new StopWatch();
        LOGGER.info("Examining chunk {} for job {}", processedChunk.getChunkId(), processedChunk.getJobId());
        final ExternalChunk deliveredChunk = new ExternalChunk(
                processedChunk.getJobId(),
                processedChunk.getChunkId(), 
                ExternalChunk.Type.DELIVERED);
        deliveredChunk.setEncoding(processedChunk.getEncoding());
        
        int itemsPushed = 0;
        for (ChunkItem chunkItem : processedChunk) {
            if (chunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
                ChunkItem deliveredItem = executeUpdateOperation(chunkItem, processedChunk.getJobId(), processedChunk.getChunkId());
                deliveredChunk.insertItem(deliveredItem);
                itemsPushed++;
            } else {
                deliveredChunk.insertItem(newIgnoredChunkItem(chunkItem.getId(),
                        String.format("Processor item status was: %s", chunkItem.getStatus())));
            }
        }
        LOGGER.info("Pushed {} items from chunk {} for job {} in {} ms",
                itemsPushed, processedChunk.getChunkId(), processedChunk.getJobId(), stopWatch.getElapsedTime());

        return deliveredChunk;
    }

    private ChunkItem executeUpdateOperation(ChunkItem chunkItem, long jobId, long chunkId) throws WebServiceException {
        final String trackingId = String.format("%d-%d-%d", jobId, chunkId, chunkItem.getId());
        final FbsUpdateConnector connector = fbsUpdateConnector.getConnector();
        ChunkItem deliveredItem;
        try {
            final UpdateMarcXchangeResult updateMarcXchangeResult = connector.updateMarcExchange(
                    StringUtil.asString(chunkItem.getData()), trackingId);
            switch(updateMarcXchangeResult.getUpdateMarcXchangeStatus()) {
                case OK:
                    deliveredItem = newSuccessfulChunkItem(
                            chunkItem.getId(), updateMarcXchangeResult.getUpdateMarcXchangeMessage());
                    break;
                case UPDATE_FAILED_PLEASE_RESEND_LATER:
                    throw new WebServiceException("Service responded with 'please resend later' message");
                default:
                    deliveredItem = newFailedChunkItem(
                            chunkItem.getId(), updateMarcXchangeResult.getUpdateMarcXchangeMessage());
            }
        } catch (WebServiceException e) {
            LOGGER.error("WebServiceException caught when handling Item {} for chunk {} for job {}",
                    chunkItem.getId(), chunkId, jobId, e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Item {} registered as FAILED for chunk {} for job {} due to exception",
                    chunkItem.getId(), chunkId, jobId, e);
            deliveredItem = newFailedChunkItem(chunkItem.getId(), ServiceUtil.stackTraceToString(e));
        }
        return deliveredItem;
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
        return new ChunkItem(chunkItemId, StringUtil.asBytes(data), status);
    }
}
