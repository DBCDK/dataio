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

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

@Entity
@Table(name = "reordereditem")
@NamedQueries({
    @NamedQuery(name = ReorderedItemEntity.GET_ITEMS_COUNT_BY_JOBID_QUERY_NAME, query = ReorderedItemEntity.GET_ITEMS_COUNT_BY_JOBID_QUERY),
    @NamedQuery(name = ReorderedItemEntity.GET_NEXT_ITEM_BY_JOBID_QUERY_NAME, query = ReorderedItemEntity.GET_NEXT_ITEM_BY_JOBID_QUERY)
})
@SqlResultSetMapping(name="ReorderedItemEntity", entities = {
    @EntityResult(entityClass=ReorderedItemEntity.class)}
)
@NamedNativeQueries({
    @NamedNativeQuery(name = ReorderedItemEntity.QUERY_GET_PARENT,
                query = "SELECT * FROM reorderedItem WHERE jobId = ? AND recordInfo @> ?::jsonb ORDER BY id DESC",
                resultSetMapping = "ReorderedItemEntity")
})
public class ReorderedItemEntity {
    public static final String GET_ITEMS_COUNT_BY_JOBID_QUERY_NAME = "ReorderedItemEntity.getItemsCountByJobId";
    public static final String GET_ITEMS_COUNT_BY_JOBID_QUERY = "SELECT COUNT(e) FROM ReorderedItemEntity e WHERE e.jobId = :jobId";
    public static final String GET_NEXT_ITEM_BY_JOBID_QUERY_NAME = "ReorderedItemEntity.getNextItemByJobId";
    public static final String GET_NEXT_ITEM_BY_JOBID_QUERY = "SELECT e FROM ReorderedItemEntity e WHERE e.jobId = :jobId ORDER BY e.sortKey ASC, e.id ASC";
    public static final String QUERY_GET_PARENT = "ReorderedItemEntity.getParent";

     /* Be advised that updating the internal state of a 'json' column
       will not mark the field as dirty and therefore not result in a
       database update. The only way to achieve an update is to replace
       the field value with a new instance (long live copy constructors).
     */

    // JPA entities need to have a primary key
    @Id
    @SequenceGenerator(
            name = "reordereditem_id_seq",
            sequenceName = "reordereditem_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "reordereditem_id_seq")
    @Column(updatable = false)
    private int id;

    private int jobId;

    private int sortKey;

    private int positionInDatafile;

    @Convert(converter = ChunkItemConverter.class)
    private ChunkItem chunkItem;

    @Convert(converter = RecordInfoConverter.class)
    private MarcRecordInfo recordInfo;

    public int getId() {
        return id;
    }

    public int getJobId() {
        return jobId;
    }

    public ReorderedItemEntity withJobId(int jobId) {
        this.jobId = jobId;
        return this;
    }

    public int getSortKey() {
        return sortKey;
    }

    public ReorderedItemEntity withSortkey(int sortkey) {
        this.sortKey = sortkey;
        return this;
    }

    public ChunkItem getChunkItem() {
        return chunkItem;
    }

    public ReorderedItemEntity withChunkItem(ChunkItem chunkItem) {
        this.chunkItem = chunkItem;
        return this;
    }

    public MarcRecordInfo getRecordInfo() {
        return recordInfo;
    }

    public ReorderedItemEntity withRecordInfo(MarcRecordInfo recordInfo) {
        this.recordInfo = recordInfo;
        return this;
    }

    public int getPositionInDatafile() {
        return positionInDatafile;
    }

    public ReorderedItemEntity withPositionInDatafile(int positionInDatafile) {
        this.positionInDatafile = positionInDatafile;
        return this;
    }
}
