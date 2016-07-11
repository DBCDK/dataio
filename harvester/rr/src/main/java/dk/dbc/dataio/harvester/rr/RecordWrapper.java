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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;

import java.util.Optional;

/**
 * This class acts as a wrapper for records in the rawrepo
 */
public class RecordWrapper {
    private final RecordId recordId;
    private Record record;
    private HarvesterException error;

    public RecordWrapper(RecordId recordId) {
        this.recordId = recordId;
    }

    public RecordId getRecordId() {
        return recordId;
    }

    public RecordWrapper withRecord(Record record) {
        this.record = record;
        return this;
    }

    public Optional<Record> getRecord() {
        return Optional.ofNullable(record);
    }

    public RecordWrapper withError(HarvesterException error) {
        this.error = error;
        return this;
    }

    public HarvesterException getError() {
        return error;
    }

    @Override
    public String toString() {
        return recordId.toString();
    }
}
