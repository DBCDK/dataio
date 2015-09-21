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

import java.util.LinkedList;
import java.util.List;

public class MockedWriteAheadLog implements WriteAheadLog {
    public LinkedList<Modification> modifications = new LinkedList<>();
    public int modificationsAddedOverTime = 0;

    @Override
    public void add(List<Modification> modifications) {
        modificationsAddedOverTime += modifications.size();
        this.modifications.addAll(modifications);
    }

    @Override
    public Modification next() throws ModificationLockedException {
        if (!modifications.isEmpty()) {
            final Modification modification = modifications.peek();
            modification.lock();
            return modification;
        }
        return null;
    }

    @Override
    public void delete(Modification modification) {
        modifications.remove(modification);
    }

    @Override
    public boolean unlock(Modification modification) {
        if (modification.isLocked()) {
            modification.unlock();
            return true;
        }
        return false;
    }
}
