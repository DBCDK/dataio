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

/**
 * Iterator for {@link Record}s in a tickle-repo batch
 * <p>
 * This class is not thread safe.
 * </p>
 */
public class BatchRecordsIterator implements RecordsIterator {
    private final TickleRepo tickleRepo;
    private final Batch batch;
    private TickleRepo.ResultSet<Record> resultSet;

    public BatchRecordsIterator(TickleRepo tickleRepo, Batch batch) {
        this.tickleRepo = tickleRepo;
        this.batch = batch;
    }

    @Override
    public Iterator<Record> iterator() {
        close();
        resultSet = tickleRepo.getRecordsInBatch(batch);
        return resultSet.iterator();
    }

    @Override
    public void close() {
        if (resultSet != null) {
            resultSet.close();
            resultSet = null;
        }
    }
}
