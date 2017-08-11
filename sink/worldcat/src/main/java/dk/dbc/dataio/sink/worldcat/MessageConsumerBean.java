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

package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@MessageDriven
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @EJB WorldCatConfigBean worldCatConfgBean;

    @EJB OcnRepo ocnRepo;

    @Stopwatch
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, NullPointerException, ServiceException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Chunk {} in job {} received successfully", chunk.getChunkId(), chunk.getJobId());
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), chunk.getType());
        final List<ChunkItem> chunkItems = new ArrayList<>();
        try {
            for(ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                final List<ChunkItemWithWorldCatAttributes> chunkItemsWithWorldCatAttributes = ChunkItemWithWorldCatAttributes.of(chunkItem);
                final Pid pid = Pid.of(chunkItemsWithWorldCatAttributes.get(0).getWorldCatAttributes().getPid());
                final WorldCatEntity worldCatEntity = new WorldCatEntity().withPid(pid.toString());
                final List<WorldCatEntity> worldCatEntities = ocnRepo.lookupWorldCatEntity(worldCatEntity);

                if(worldCatEntities == null || worldCatEntities.isEmpty()) {
                    worldCatEntity.withChecksum(0).withAgencyId(pid.getAgencyId()).withBibliographicRecordId(pid.getBibliographicRecordId());
                    ocnRepo.getEntityManager().persist(worldCatEntity);

                } else {
                    if(worldCatEntities.size() > 1) {
                        throw new IllegalStateException("Found more than one worldCat entity");
                    }
                }

                WciruServiceBroker wciruServiceBroker = new WciruServiceBroker(worldCatConfgBean.wciruServiceConnector);
                chunkItems.add(wciruServiceBroker.push(chunkItemsWithWorldCatAttributes.get(0), worldCatEntity));
            }
            result.addAllItems(chunkItems);
        } catch (JSONBException | IOException e) {
            // TODO: 08/03/2017
            LOGGER.error(e.toString());
        } finally {
            DBCTrackedLogContext.remove();
        }
    }
}
