package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.ticklerepo.dto.Record;

public interface RecordsIterator extends Iterable<Record> {
    default void close() {
    }

    default void commit() {
    }

    default Integer getBasedOn() {
        return null;
    }
}
