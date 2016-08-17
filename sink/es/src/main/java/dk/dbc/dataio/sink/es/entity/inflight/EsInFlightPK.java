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

package dk.dbc.dataio.sink.es.entity.inflight;

public class EsInFlightPK {
    private Long sinkId;
    private Integer targetReference;

    public Long getSinkId() {
        return sinkId;
    }

    public void setSinkId(Long sinkId) {
        this.sinkId = sinkId;
    }

    public Integer getTargetReference() {
        return targetReference;
    }

    public void setTargetReference(Integer targetReference) {
        this.targetReference = targetReference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EsInFlightPK)) return false;

        EsInFlightPK that = (EsInFlightPK) o;

        if (sinkId != null ? !sinkId.equals(that.sinkId) : that.sinkId != null) return false;
        return targetReference != null ? targetReference.equals(that.targetReference) : that.targetReference == null;

    }

    @Override
    public int hashCode() {
        int result = sinkId != null ? sinkId.hashCode() : 0;
        result = 31 * result + (targetReference != null ? targetReference.hashCode() : 0);
        return result;
    }
}
