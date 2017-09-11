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

import java.util.Iterator;

public class BatchRecordsIterator implements RecordsIterator {
    private final TickleRepo tickleRepo;
    private final Batch batch;

    public BatchRecordsIterator(TickleRepo tickleRepo, Batch batch) {
        this.tickleRepo = tickleRepo;
        this.batch = batch;
    }

    @Override
    public Iterator<Record> iterator() {
        // We wrap this resultSetIterator instead of just returning it,
        // since we know that more methods will be added to the
        // RecordsIterator interface in the near future.
        final Iterator<Record> resultSetIterator = tickleRepo.getRecordsInBatch(batch).iterator();

        return new Iterator<Record>() {
            @Override
            public boolean hasNext() {
                return resultSetIterator.hasNext();
            }

            @Override
            public Record next() {
                return resultSetIterator.next();
            }
        };
    }
}
