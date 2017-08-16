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
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.oclc.wciru.WciruServiceConnector;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.util.HashSet;
import java.util.List;

@MessageDriven
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    @EJB WorldCatConfigBean worldCatConfgBean;
    @EJB OcnRepo ocnRepo;

    WorldCatSinkConfig config;
    WciruServiceConnector connector;

    @Stopwatch
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, NullPointerException, ServiceException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        refreshConfigIfOutdated(consumedMessage);

        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), chunk.getType());
        try {
            for (ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                result.insertItem(handleChunkItem(chunkItem));
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
    }

    private void refreshConfigIfOutdated(ConsumedMessage consumedMessage) throws SinkException {
        final WorldCatSinkConfig latestConfig = worldCatConfgBean.getConfig(consumedMessage);
        if (!latestConfig.equals(config)) {
            LOGGER.debug("Updating WCIRU connector");
            connector = getWciruServiceConnector(latestConfig);
            config = latestConfig;
        }
    }

    private WciruServiceConnector getWciruServiceConnector(WorldCatSinkConfig config) {
        final WciruServiceConnector.RetryScheme retryScheme = new WciruServiceConnector.RetryScheme(
                1,              // maxNumberOfRetries
                1000, // milliSecondsToSleepBetweenRetries
                new HashSet<>(config.getRetryDiagnostics()));

        return new WciruServiceConnector(
                config.getEndpoint(),
                config.getUserId(),
                config.getPassword(),
                config.getProjectId(),
                retryScheme);
    }

    private ChunkItem handleChunkItem(ChunkItem chunkItem) {
        try {
            final ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes = ChunkItemWithWorldCatAttributes.of(chunkItem);

            final Pid pid = Pid.of(chunkItemWithWorldCatAttributes.getWorldCatAttributes().getPid());

            final WorldCatEntity worldCatEntity = new WorldCatEntity().withPid(pid.toString());
            final List<WorldCatEntity> worldCatEntities = ocnRepo.lookupWorldCatEntity(worldCatEntity);
            if (worldCatEntities == null || worldCatEntities.isEmpty()) {
                worldCatEntity.withChecksum(0).withAgencyId(pid.getAgencyId()).withBibliographicRecordId(pid.getBibliographicRecordId());
                ocnRepo.getEntityManager().persist(worldCatEntity);

            } else {
                if (worldCatEntities.size() > 1) {
                    throw new IllegalStateException("Found more than one worldCat entity");
                }
            }

            //final WciruServiceBroker wciruServiceBroker = new WciruServiceBroker(connector);
            //chunkItems.add(wciruServiceBroker.push(chunkItemsWithWorldCatAttributes.get(0), worldCatEntity));
            // TODO: 16-08-17 Update to new broker API
            // TODO: 16-08-17 Handle potential deletion of WorldCatEntity
            // TODO: 16-08-17 Update WorldCatEntity OCN based on broker result

            // TODO: 16-08-17 Replace with formatted output
            return null;
        } catch (IllegalArgumentException e) {
            // TODO: 16-08-17 fail this chunk item
            throw new IllegalStateException("work-in-progress");
        }
    }
}
