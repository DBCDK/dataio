/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.commons.types;

import java.io.Serializable;

/**
 * Brief view for FlowComponent DTO
 */
public class FlowComponentView implements Serializable {
    private long id;
    private long version;
    private String revision;
    private String nextRevision;

    public long getId() {
        return id;
    }

    public FlowComponentView withId(long id) {
        this.id = id;
        return this;
    }

    public long getVersion() {
        return version;
    }

    public FlowComponentView withVersion(long version) {
        this.version = version;
        return this;
    }

    public String getRevision() {
        return revision;
    }

    public void withRevision(String revision) {
        this.revision = revision;
    }

    public String getNextRevision() {
        return nextRevision;
    }

    public void setNextRevision(String nextRevision) {
        this.nextRevision = nextRevision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FlowComponentView that = (FlowComponentView) o;

        if (id != that.id) {
            return false;
        }
        if (version != that.version) {
            return false;
        }
        if (revision != null ? !revision.equals(that.revision) : that.revision != null) {
            return false;
        }
        return nextRevision != null ? nextRevision.equals(that.nextRevision) : that.nextRevision == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + (revision != null ? revision.hashCode() : 0);
        result = 31 * result + (nextRevision != null ? nextRevision.hashCode() : 0);
        return result;
    }
}