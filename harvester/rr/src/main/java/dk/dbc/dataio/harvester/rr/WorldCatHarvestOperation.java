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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

public class WorldCatHarvestOperation extends HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCatHarvestOperation.class);

    private final OcnRepo ocnRepo;

    public WorldCatHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, EntityManager harvestTaskEntityManager, OcnRepo ocnRepo) {
        this(config, harvesterJobBuilderFactory, harvestTaskEntityManager, null, null, ocnRepo);
    }

    WorldCatHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, EntityManager harvestTaskEntityManager,
                        AgencyConnection agencyConnection, RawRepoConnector rawRepoConnector, OcnRepo ocnRepo) {
        super(config, harvesterJobBuilderFactory, harvestTaskEntityManager, agencyConnection, rawRepoConnector);
        this.ocnRepo = ocnRepo;
    }

    /**
     * Runs this harvest operation, creating dataIO jobs from harvested records.
     * If any non-internal error occurs a record is marked as failed.
     * @return number of records processed
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Override
    public int execute() throws HarvesterException {
        final StopWatch stopWatch = new StopWatch();
        final RecordHarvestTaskQueue recordHarvestTaskQueue = createTaskQueue();

        int itemsProcessed = 0;
        RawRepoRecordHarvestTask recordHarvestTask = recordHarvestTaskQueue.poll();
        while (recordHarvestTask != null) {
            LOGGER.info("{} ready for harvesting", recordHarvestTask.getRecordId());
            for (RawRepoRecordHarvestTask task : preprocessRecordHarvestTask(recordHarvestTask)) {
                LOGGER.info("handling pid {} ocn {}", task.getAddiMetaData().pid(), task.getAddiMetaData().ocn());
                processRecordHarvestTask(task);
                itemsProcessed++;
            }
            recordHarvestTask = recordHarvestTaskQueue.poll();
        }
        flushHarvesterJobBuilders();

        recordHarvestTaskQueue.commit();

        LOGGER.info("Processed {} items from {} queue in {} ms",
                itemsProcessed, configContent.getConsumerId(), stopWatch.getElapsedTime());

        return itemsProcessed;
    }

    List<RawRepoRecordHarvestTask> preprocessRecordHarvestTask(RawRepoRecordHarvestTask task) {
        final List<RawRepoRecordHarvestTask> tasks = getWorldCatEntities(task).stream()
                .map(worldCatEntity -> mergeTaskWithWorldCatEntity(task, worldCatEntity))
                .filter(t -> hasPid(t.getAddiMetaData()))
                .collect(Collectors.toList());

        if (tasks.isEmpty() && hasPid(task.getAddiMetaData())) {
            tasks.add(task);
        }

        return tasks;
    }

    private List<WorldCatEntity> getWorldCatEntities(RawRepoRecordHarvestTask task) {
        final AddiMetaData addiMetaData = task.getAddiMetaData();
        if (hasPid(addiMetaData)) {
            return ocnRepo.lookupWorldCatEntity(new WorldCatEntity()
                                .withPid(addiMetaData.pid()));
        }
        return ocnRepo.lookupWorldCatEntity(new WorldCatEntity()
                            .withAgencyId(addiMetaData.submitterNumber())
                            .withBibliographicRecordId(addiMetaData.bibliographicRecordId()));
    }

    private RawRepoRecordHarvestTask mergeTaskWithWorldCatEntity(RawRepoRecordHarvestTask task, WorldCatEntity worldCatEntity) {
        return new RawRepoRecordHarvestTask()
                .withRecordId(task.getRecordId())
                .withAddiMetaData(new AddiMetaData()
                        .withPid(worldCatEntity.getPid())
                        .withOcn(worldCatEntity.getOcn())
                        .withSubmitterNumber(worldCatEntity.getAgencyId())
                        .withBibliographicRecordId(worldCatEntity.getBibliographicRecordId()));
    }

    private boolean hasPid(AddiMetaData addiMetaData) {
        return addiMetaData.pid() != null && !addiMetaData.pid().trim().isEmpty();
    }
}
