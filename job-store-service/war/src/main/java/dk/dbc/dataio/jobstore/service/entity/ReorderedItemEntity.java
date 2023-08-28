package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

@Entity
@Table(name = "reordereditem")
@NamedQueries({
        @NamedQuery(name = ReorderedItemEntity.GET_ITEMS_COUNT_BY_JOBID_QUERY_NAME, query = ReorderedItemEntity.GET_ITEMS_COUNT_BY_JOBID_QUERY),
        @NamedQuery(name = ReorderedItemEntity.GET_NEXT_ITEM_BY_JOBID_QUERY_NAME, query = ReorderedItemEntity.GET_NEXT_ITEM_BY_JOBID_QUERY)
})
@SqlResultSetMapping(name = "ReorderedItemEntity", entities = {
        @EntityResult(entityClass = ReorderedItemEntity.class)}
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
