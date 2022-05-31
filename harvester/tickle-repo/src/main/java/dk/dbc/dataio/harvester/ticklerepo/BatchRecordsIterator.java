package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.Record;

import java.util.Collections;
import java.util.Iterator;

/**
 * One-time iterator for {@link Record}s in a tickle-repo batch
 * <p>
 * This class is not thread safe.
 * </p>
 */
public class BatchRecordsIterator implements RecordsIterator {
    private final TickleRepo.ResultSet<Record> resultSet;
    private final Iterator<Record> recordsIterator;

    public BatchRecordsIterator(TickleRepo tickleRepo, Batch batch) {
        if (batch != null) {
            resultSet = tickleRepo.getRecordsInBatch(batch);
            recordsIterator = resultSet.iterator();
        } else {
            resultSet = null;
            recordsIterator = Collections.emptyIterator();
        }
    }

    @Override
    public Iterator<Record> iterator() {
        return recordsIterator;
    }

    @Override
    public void close() {
        if (resultSet != null) {
            resultSet.close();
        }
    }
}
