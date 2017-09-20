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
import dk.dbc.dataio.harvester.types.HarvestTaskSelector;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRecordsIterator.class);

    private final TickleRepo tickleRepo;
    private final TaskRepo taskRepo;
    private final TickleRepoHarvesterConfig config;
    private final HarvestTask task;
    private final int dataSetId;
    private final Iterator<Record> recordsIterator;

    public TaskRecordsIterator(TickleRepo tickleRepo, TaskRepo taskRepo, TickleRepoHarvesterConfig config) {
        this.tickleRepo = tickleRepo;
        this.taskRepo = taskRepo;
        this.config = config;
        this.task = taskRepo.findNextHarvestTask(config.getId()).orElse(null);
        this.dataSetId = getDataSet().map(DataSet::getId).orElse(0);
        if (task != null) {
            this.recordsIterator = createIteratorFromTask(task);
        } else {
            this.recordsIterator = Collections.emptyIterator();
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

    @Override
    public void close() {
        if (recordsIterator instanceof IterateBy) {
            ((IterateBy) recordsIterator).close();
        }
    }

    private Iterator<Record> createIteratorFromTask(HarvestTask task) {
        if (task.getSelector() != null) {
            return createIteratorFromSelector(task.getSelector());
        }
        if (task.getRecords() != null) {
            return new IterateByList(task.getRecords());
        }
        return Collections.emptyIterator();
    }

    private Iterator<Record> createIteratorFromSelector(HarvestTaskSelector selector) {
        if ("datasetname".equalsIgnoreCase(selector.getField())) {
            if (!config.getContent().getDatasetName().equals(selector.getValue())) {
                LOGGER.error("Data set name selector mismatch: config:{} != selector:{}",
                        config.getContent().getDatasetName(), selector.getField());
                return Collections.emptyIterator();
            }
            return new IterateByDataSet();
        }
        if ("dataset".equalsIgnoreCase(selector.getField())) {
            if (!Integer.toString(dataSetId).equals(selector.getValue())) {
                LOGGER.error("Data set selector mismatch: config:{} != selector:{}",
                        dataSetId, selector.getValue());
                return Collections.emptyIterator();
            }
            return new IterateByDataSet();
        }
        LOGGER.error("Unknown selector field: {}", selector.getField());
        return Collections.emptyIterator();
    }

    private Optional<DataSet> getDataSet() {
        return tickleRepo.lookupDataSet(new DataSet().withName(config.getContent().getDatasetName()));
    }

    private abstract class IterateBy implements Iterator<Record> {
        public void close() {}
    }

    public class IterateByList extends IterateBy {
        private final Iterator<AddiMetaData> list;

        public IterateByList(List<AddiMetaData> records) {
            list = records.iterator();
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
    }

    public class IterateByDataSet extends IterateBy {
        private final TickleRepo.ResultSet<Record> resultSet;
        private final Iterator<Record> resultSetIterator;

        public IterateByDataSet() {
            resultSet = tickleRepo.getRecordsInDataSet(new DataSet().withId(dataSetId));
            resultSetIterator = resultSet.iterator();
        }

        @Override
        public boolean hasNext() {
            return resultSetIterator.hasNext();
        }

        @Override
        public Record next() {
            return resultSetIterator.next();
        }

        @Override
        public void close() {
            resultSet.close();
        }
    }
}
