package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.Flow;

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
@SqlResultSetMapping(name="FlowCacheEntity.implicit", entities = {
    @EntityResult(entityClass=FlowCacheEntity.class)}
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
