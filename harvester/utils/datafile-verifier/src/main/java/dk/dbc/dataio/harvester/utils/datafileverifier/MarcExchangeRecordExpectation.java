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

package dk.dbc.dataio.harvester.utils.datafileverifier;

import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;

import java.util.Optional;

/**
 * Verifier helper class for MARC exchange collection record members
 */
public class MarcExchangeRecordExpectation {
    private final String bibliographicRecordId;
    private final int agencyId;

    public MarcExchangeRecordExpectation(MarcRecord marcRecord) {
        final DataField f001 = (DataField) getField(marcRecord, "001")
                .orElseThrow(() -> new IllegalArgumentException("field 001 not found"));
        this.bibliographicRecordId = getSubfield(f001, 'a')
                .orElseThrow(() -> new IllegalArgumentException("001a not found"))
                .getData();
        this.agencyId = Integer.parseInt(getSubfield(f001, 'b')
                .orElseThrow(() -> new IllegalArgumentException("001b not found"))
                .getData());
    }

    public MarcExchangeRecordExpectation(String bibliographicRecordId, int agencyId) {
        this.bibliographicRecordId = bibliographicRecordId;
        this.agencyId = agencyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MarcExchangeRecordExpectation that = (MarcExchangeRecordExpectation) o;

        if (agencyId != that.agencyId) {
            return false;
        }
        if (bibliographicRecordId != null ? !bibliographicRecordId.equals(that.bibliographicRecordId) : that.bibliographicRecordId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = bibliographicRecordId != null ? bibliographicRecordId.hashCode() : 0;
        result = 31 * result + agencyId;
        return result;
    }

    @Override
    public String toString() {
        return "MarcExchangeRecordExpectation{" +
                "bibliographicRecordId='" + bibliographicRecordId + '\'' +
                ", agencyId=" + agencyId +
                '}';
    }

    private Optional<Field> getField(MarcRecord marcRecord, String tag) {
        return marcRecord.getFields().stream().filter(field -> tag.equals(field.getTag())).findFirst();
    }

    private Optional<SubField> getSubfield(DataField field, char code) {
        return field.getSubfields().stream().filter(s -> s.getCode() == code).findFirst();
    }
}
