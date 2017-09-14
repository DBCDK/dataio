/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * One-time iterator for {@link Record}s in a tickle-repo harvester task
 * <p>
 * This class is not thread safe.
 * </p>
 */
public class TaskRecordsIterator implements RecordsIterator {
    private final TickleRepo tickleRepo;
    private final TaskRepo taskRepo;
    private final TickleRepoHarvesterConfig config;
    private final HarvestTask task;
    private final Iterator<Record> recordsIterator;

    public TaskRecordsIterator(TickleRepo tickleRepo, TaskRepo taskRepo, TickleRepoHarvesterConfig config) {
        this.tickleRepo = tickleRepo;
        this.taskRepo = taskRepo;
        this.config = config;
        this.task = taskRepo.findNextHarvestTask(config.getId()).orElse(null);
        if (task != null) {
            recordsIterator = new ByList(task.getRecords());
        } else {
            recordsIterator = Collections.emptyIterator();
        }
    }

    @Override
    public Iterator<Record> iterator() {
        return recordsIterator;
    }

    @Override
    public void commit() {
        if (task != null) {
            taskRepo.getEntityManager().remove(task);
        }
    }

    public class ByList implements Iterator<Record> {
        private final Iterator<AddiMetaData> list;
        private final int dataSetId;

        public ByList(List<AddiMetaData> records) {
            list = records.iterator();
            dataSetId = getDataSet().map(DataSet::getId).orElse(0);
        }

        @Override
        public boolean hasNext() {
            return list.hasNext();
        }

        @Override
        public Record next() {
            final AddiMetaData metaData = list.next();
            if (metaData == null) {
                return null;
            }

            return tickleRepo.lookupRecord(new Record()
                    .withDataset(dataSetId)
                    .withLocalId(metaData.bibliographicRecordId()))
                    .orElse(null);
        }

        private Optional<DataSet> getDataSet() {
            return tickleRepo.lookupDataSet(new DataSet().withName(config.getContent().getDatasetName()));
        }
    }
}
