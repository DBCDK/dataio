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
