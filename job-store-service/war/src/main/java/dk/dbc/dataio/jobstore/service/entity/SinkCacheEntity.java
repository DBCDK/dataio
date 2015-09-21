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

package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.Sink;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

@Entity
@SqlResultSetMapping(name = "SinkCacheEntity.implicit", entities = {
    @EntityResult(entityClass=SinkCacheEntity.class)}
)
@NamedNativeQueries({
    @NamedNativeQuery(name = SinkCacheEntity.NAMED_QUERY_SET_CACHE,
        query = "select * from set_sinkcache(?checksum, ?sink)", resultSetMapping = "SinkCacheEntity.implicit")
})
@Table(name = "sinkcache")
public class SinkCacheEntity {
    public static final String NAMED_QUERY_SET_CACHE = "SinkCacheEntity.set";

    @Id
    @SequenceGenerator(
            name = "sinkcache_id_seq",
            sequenceName = "sinkcache_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sinkcache_id_seq")
    @Column(updatable = false)
    private int id;

    private String checksum;

    @Convert(converter = SinkConverter.class)
    private Sink sink;

    public int getId() {
        return id;
    }

    public String getChecksum() {
        return checksum;
    }

    public Sink getSink() {
        return sink;
    }
}
