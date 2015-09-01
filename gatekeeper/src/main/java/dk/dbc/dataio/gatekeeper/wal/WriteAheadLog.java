package dk.dbc.dataio.gatekeeper.wal;

import java.util.List;

public interface WriteAheadLog {
    /**
     * Commits list of modifications to the write-ahead-log
     * @param modifications list of modifications
     */
    void add(List<Modification> modifications);

    /**
     * Locks and returns next modification from the write-ahead-log
     * @return next modification from the write-ahead-log
     * @throws ModificationLockedException if next modification is already locked
     */
    Modification next() throws ModificationLockedException;

    /**
     * Deletes given modification from the write-aheafd-log
     * @param modification modification to delete
     */
    void delete(Modification modification);
}
