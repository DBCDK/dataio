package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.Sink;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

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
