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
        private Record next;

        public IterateByList(List<AddiMetaData> records) {
            list = records.iterator();
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            boolean hasNext;
            do {
                hasNext = list.hasNext();
                if (hasNext) {
                    next = resolveNext();
                }
            } while (hasNext && next == null);
            return next != null;
        }

        @Override
        public Record next() {
            try {
                return next != null ? next : resolveNext();
            } finally {
                next = null;
            }
        }

        private Record resolveNext() {
            final AddiMetaData metaData = list.next();
            if (metaData == null) {
                return null;
            }

            final Record record = tickleRepo.lookupRecord(new Record()
                    .withDataset(dataSetId)
                    .withLocalId(metaData.bibliographicRecordId()))
                    .orElse(null);
            if (record == null) {
                LOGGER.warn("Record with bibliographicRecordId '{}' was not found in dataset {}",
                        metaData.bibliographicRecordId(), dataSetId);
            }
            return record;
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
