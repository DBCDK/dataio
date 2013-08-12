package dk.dbc.dataio.flowstore.entity;

import java.util.Date;

/**
 * Representation of Entity composite key (id, version)
 */
public class EntityPrimaryKey {
    private final Long id;
    private final Date version;

    public EntityPrimaryKey(Long id, Date version) {
        this.id = id;
        this.version = new Date(version.getTime());
    }

    public Long getId() {
        return id;
    }

    public Date getVersion() {
        return new Date(version.getTime());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EntityPrimaryKey that = (EntityPrimaryKey) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}
