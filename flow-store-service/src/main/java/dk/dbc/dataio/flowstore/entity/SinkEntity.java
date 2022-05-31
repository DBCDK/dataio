package dk.dbc.dataio.flowstore.entity;

import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

@Entity
@SqlResultSetMapping(name = "Sink.implicit", entities = {
        @EntityResult(entityClass = SinkEntity.class)}
)
@NamedNativeQueries({
        @NamedNativeQuery(name = SinkEntity.QUERY_FIND_ALL,
                query = "select * from sinks order by upper(content ->> 'name') asc", resultSetMapping = "Sink.implicit")
})
@Table(name = "sinks")
public class SinkEntity extends Versioned {
    public static final String QUERY_FIND_ALL = "Sink.findAll";
}
