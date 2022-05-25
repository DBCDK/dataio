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
        @EntityResult(entityClass = SinkCacheEntity.class)}
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

    /* for test */
    public static SinkCacheEntity create(Sink sink) {
        final SinkCacheEntity sinkCacheEntity = new SinkCacheEntity();
        sinkCacheEntity.sink = sink;
        return sinkCacheEntity;
    }
}
