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
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.sink.fbs.connector.FbsUpdateConnector;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.xml.ws.WebServiceException;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.asBytes;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.asString;

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
    public Chunk push(Chunk processedChunk) throws WebServiceException {
        final StopWatch stopWatch = new StopWatch();
        LOGGER.info("Examining chunk {} for job {}", processedChunk.getChunkId(), processedChunk.getJobId());
        final Chunk chunkForDelivery = new Chunk(processedChunk.getJobId(), processedChunk.getChunkId(), Chunk.Type.DELIVERED);
        chunkForDelivery.setEncoding(processedChunk.getEncoding());
        
        int numberOfItemsPushed = 0;
        for (ChunkItem processedChunkItem : processedChunk) {
            if (processedChunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
                executeUpdateOperation(processedChunkItem, chunkForDelivery);
                numberOfItemsPushed++;
            } else {
                chunkForDelivery.addItemWithStatusIgnored( processedChunkItem.getId(), asBytes(String.format("Processor item status was: %s", processedChunkItem.getStatus())) );
            }
        }

        LOGGER.info("Pushed {} items from chunk {} for job {} in {} ms", numberOfItemsPushed, processedChunk.getChunkId(), processedChunk.getJobId(), stopWatch.getElapsedTime());
        return chunkForDelivery;
    }

    private void executeUpdateOperation(ChunkItem processedChunkItem, Chunk chunkForDelivery) throws WebServiceException {
        final String trackingId = String.format("%d-%d-%d", chunkForDelivery.getJobId(), chunkForDelivery.getChunkId(), processedChunkItem.getId());
        final FbsUpdateConnector connector = fbsUpdateConnector.getConnector();
        try {
            final UpdateMarcXchangeResult updateMarcXchangeResult = connector.updateMarcExchange(asString(processedChunkItem.getData()), trackingId);
            switch(updateMarcXchangeResult.getUpdateMarcXchangeStatus()) {
                case OK:
                    chunkForDelivery.addItemWithStatusSuccess(processedChunkItem.getId(), asBytes(updateMarcXchangeResult.getUpdateMarcXchangeMessage()));
                    break;
                case UPDATE_FAILED_PLEASE_RESEND_LATER:
                    throw new WebServiceException("Service responded with 'please resend later' message");
                default:
                    chunkForDelivery.addItemWithStatusFailed(processedChunkItem.getId(), asBytes(updateMarcXchangeResult.getUpdateMarcXchangeMessage()));
            }
        } catch (WebServiceException e) {
            LOGGER.error("WebServiceException caught when handling Item {} for chunk {} for job {}", processedChunkItem.getId(), chunkForDelivery.getChunkId(), chunkForDelivery.getJobId(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Item {} registered as FAILED for chunk {} for job {} due to exception", processedChunkItem.getId(), chunkForDelivery.getChunkId(), chunkForDelivery.getJobId(), e);
            chunkForDelivery.addItemWithStatusFailed(processedChunkItem.getId(), asBytes(ServiceUtil.stackTraceToString(e)));

        }
    }
}