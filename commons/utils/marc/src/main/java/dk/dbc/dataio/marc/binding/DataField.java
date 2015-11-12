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

public class DataField extends Field<DataField> {
    private final List<SubField> subfields;
    private char ind1;
    private char ind2;

    public DataField() {
        this.subfields = new ArrayList<>();
    }

    public List<SubField> getSubfields() {
        return subfields;
    }

    public DataField addSubfield(SubField subField) {
        subfields.add(subField);
        return this;
    }

    public DataField addAllSubFields(Collection<SubField> subs) {
        subfields.addAll(subs);
        return this;
    }

    public char getInd1() {
        return ind1;
    }

    public DataField setInd1(char ind1) {
        this.ind1 = ind1;
        return this;
    }

    public char getInd2() {
        return ind2;
    }

    public DataField setInd2(char ind2) {
        this.ind2 = ind2;
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

        DataField dataField = (DataField) o;

        if (ind1 != dataField.ind1) {
            return false;
        }
        if (ind2 != dataField.ind2) {
            return false;
        }
        return !(subfields != null ? !subfields.equals(dataField.subfields) : dataField.subfields != null);

    }

    @Override
    public int hashCode() {
        int result = subfields != null ? subfields.hashCode() : 0;
        result = 31 * result + (int) ind1;
        result = 31 * result + (int) ind2;
        return result;
    }

    @Override
    public String toString() {
        return "DataField{" +
                "tag=" + tag +
                ", ind1=" + ind1 +
                ", ind2=" + ind2 +
                ", subfields=" + subfields +
                '}';
    }
}
