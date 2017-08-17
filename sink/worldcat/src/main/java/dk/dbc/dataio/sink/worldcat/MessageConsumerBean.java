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
    WciruServiceBroker wciruServiceBroker;

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
            wciruServiceBroker = new WciruServiceBroker(connector);
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
            final ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes =
                    ChunkItemWithWorldCatAttributes.of(chunkItem);
            final Pid pid = Pid.of(chunkItemWithWorldCatAttributes.getWorldCatAttributes().getPid());
            final WorldCatEntity worldCatEntity = getWorldCatEntity(pid);
            final WciruServiceBroker.Result brokerResult =
                    wciruServiceBroker.push(chunkItemWithWorldCatAttributes, worldCatEntity);

            worldCatEntity.withOcn(brokerResult.getOcn());

            if (!brokerResult.isFailed()
                    && brokerResult.getLastEvent().getAction() == WciruServiceBroker.Event.Action.DELETE) {
                LOGGER.info("Deletion of PID '{}' triggered WorldCat entry removal in repository", pid);
                ocnRepo.getEntityManager().remove(worldCatEntity);
            }

            return FormattedOutput.of(pid, brokerResult)
                    .withId(chunkItem.getId())
                    .withTrackingId(chunkItem.getTrackingId());
        } catch (IllegalArgumentException e) {
            return FormattedOutput.of(e)
                    .withId(chunkItem.getId())
                    .withTrackingId(chunkItem.getTrackingId());
        }
    }

    private WorldCatEntity getWorldCatEntity(Pid pid) {
        final WorldCatEntity worldCatEntity = new WorldCatEntity().withPid(pid.toString());
        final List<WorldCatEntity> worldCatEntities = ocnRepo.lookupWorldCatEntity(worldCatEntity);
        if (worldCatEntities == null || worldCatEntities.isEmpty()) {
            // create new entry in the OCN repository
            worldCatEntity
                    .withChecksum(0)
                    .withAgencyId(pid.getAgencyId())
                    .withBibliographicRecordId(pid.getBibliographicRecordId());
            ocnRepo.getEntityManager().persist(worldCatEntity);
            return worldCatEntity;
        }

        if (worldCatEntities.size() > 1) {
            throw new IllegalStateException("PID '" + pid + "' resolved to more than one WorldCat entity");
        }
        return worldCatEntities.get(0);
    }
}
