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

package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = Sink.TABLE_NAME,
uniqueConstraints = {
    @UniqueConstraint(columnNames = { Sink.NAME_INDEX_COLUMN }),
})
@NamedQueries({
    @NamedQuery(name = Sink.QUERY_FIND_ALL, query = "SELECT sink FROM Sink sink ORDER BY sink.nameIndexValue ASC")
})
public class Sink extends VersionedEntity {
    public static final String TABLE_NAME = "sinks";
    public static final String QUERY_FIND_ALL = "Sink.findAll";
    static final String NAME_INDEX_COLUMN = "name_idx";

    @Lob
    @Column(name = NAME_INDEX_COLUMN, nullable = false)
    private String nameIndexValue;

    String getNameIndexValue() {
        return nameIndexValue;
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException if given null-valued data argument
     * @throws IllegalArgumentException if given empty-valued data argument
     * @throws JSONBException if non-json JSON string or if given JSON is invalid SinkContent.
     */
    @Override
    protected void preProcessContent(String data) throws JSONBException {
        final SinkContent sinkContent = new JSONBContext().unmarshall(data, SinkContent.class);
        nameIndexValue = sinkContent.getName();
    }
}
