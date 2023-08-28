package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.Flow;
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
@SqlResultSetMapping(name = "FlowCacheEntity.implicit", entities = {
        @EntityResult(entityClass = FlowCacheEntity.class)}
)
@NamedNativeQueries({
        @NamedNativeQuery(name = FlowCacheEntity.NAMED_QUERY_SET_CACHE,
                query = "select * from set_flowcache(?checksum, ?flow)", resultSetMapping = "FlowCacheEntity.implicit")
})
@Table(name = "flowcache")
public class FlowCacheEntity {
    public static final String NAMED_QUERY_SET_CACHE = "FlowCacheEntity.set";

    @Id
    @SequenceGenerator(
            name = "flowcache_id_seq",
            sequenceName = "flowcache_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "flowcache_id_seq")
    @Column(updatable = false)
    private int id;

    private String checksum;

    @Convert(converter = FlowConverter.class)
    private Flow flow;

    public int getId() {
        return id;
    }

    public String getChecksum() {
        return checksum;
    }

    public Flow getFlow() {
        return flow;
    }
}
