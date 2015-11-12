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

package dk.dbc.dataio.marc.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MarcRecord {
    private Leader leader;
    private final List<Field> fields;

    public MarcRecord() {
        this.fields = new ArrayList<>();
    }

    public Leader getLeader() {
        return leader;
    }

    public MarcRecord setLeader(Leader leader) {
        this.leader = leader;
        return this;
    }

    public List<Field> getFields() {
        return fields;
    }

    public MarcRecord addField(Field<? extends Field> field) {
        fields.add(field);
        return this;
    }

    public MarcRecord addAllFields(Collection<? extends Field> fields) {
        this.fields.addAll(fields);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MarcRecord that = (MarcRecord) o;

        if (leader != null ? !leader.equals(that.leader) : that.leader != null) {
            return false;
        }
        return !(fields != null ? !fields.equals(that.fields) : that.fields != null);

    }

    @Override
    public int hashCode() {
        int result = leader != null ? leader.hashCode() : 0;
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MarcRecord{" +
                "leader=" + leader +
                ", fields=" + fields +
                '}';
    }
}
