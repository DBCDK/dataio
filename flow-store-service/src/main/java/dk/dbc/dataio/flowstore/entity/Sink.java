package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;

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
     * @throws JsonException if non-json JSON string or if given JSON is invalid SinkContent.
     */
    @Override
    protected void preProcessContent(String data) throws JsonException {
        final SinkContent sinkContent = JsonUtil.fromJson(data, SinkContent.class, MixIns.getMixIns());
        nameIndexValue = sinkContent.getName();
    }
}
