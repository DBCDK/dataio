package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * Brief view for Flow DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowView implements Serializable {
    private long id;
    private long version;
    private String name;
    private String description;
    private Date timeOfLastModification;
    @JsonIgnore
    private Date timeOfComponentUpdate;

    public FlowView() {
    }

    @JsonCreator
    public FlowView(@JsonProperty("id") long id , @JsonProperty("version") long version, @JsonProperty("name") String name,
                    @JsonProperty("description") String description, @JsonProperty("timeOfLastModification") Date timeOfLastModification) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.description = description;
        this.timeOfLastModification = timeOfLastModification;
    }

    public long getId() {
        return id;
    }

    public FlowView withId(long id) {
        this.id = id;
        return this;
    }

    public long getVersion() {
        return version;
    }

    public FlowView withVersion(long version) {
        this.version = version;
        return this;
    }

    public String getName() {
        return name;
    }

    public FlowView withName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public FlowView withDescription(String description) {
        this.description = description;
        return this;
    }

    public Date getTimeOfLastModification() {
        return timeOfLastModification;
    }

    public FlowView withTimeOfLastModification(Date timeOfLastModification) {
        if (timeOfLastModification != null) {
            this.timeOfLastModification = new Date(timeOfLastModification.getTime());
        }
        return this;
    }

    public Date getTimeOfComponentUpdate() {
        return timeOfComponentUpdate;
    }

    public FlowView withTimeOfComponentUpdate(Date timeOfComponentUpdate) {
        if (timeOfComponentUpdate != null) {
            this.timeOfComponentUpdate = new Date(timeOfComponentUpdate.getTime());
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FlowView flowView = (FlowView) o;

        if (id != flowView.id) {
            return false;
        }
        if (version != flowView.version) {
            return false;
        }
        if (name != null ? !name.equals(flowView.name) : flowView.name != null) {
            return false;
        }
        if (description != null ? !description.equals(flowView.description) : flowView.description != null) {
            return false;
        }
        if (timeOfLastModification != null ? !timeOfLastModification.equals(flowView.timeOfLastModification) : flowView.timeOfLastModification != null) {
            return false;
        }
        return timeOfComponentUpdate != null ? timeOfComponentUpdate.equals(flowView.timeOfComponentUpdate) : flowView.timeOfComponentUpdate == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (timeOfLastModification!= null ? timeOfLastModification.hashCode() : 0);
        result = 31 * result + (timeOfComponentUpdate != null ? timeOfComponentUpdate.hashCode() : 0);
        return result;
    }
}
