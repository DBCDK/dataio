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
     * Deletes given modification from the write-ahead-log
     * @param modification modification to delete
     */
    void delete(Modification modification);

    /**
     * Unlocks given modification in the write-ahead-lock
     * @param modification modification to unlock
     * @return true if modification was locked and subsequently unlocked, false
     * if modification was already unlocked
     */
    boolean unlock(Modification modification);
}
