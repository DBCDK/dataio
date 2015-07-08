package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.gui.client.util.Format;

import java.util.List;

public class SinkModel extends GenericBackendModel {

    private String sinkName;
    private String resource;
    private String description;

    public SinkModel(long id, long version, String name, String resource, String description) {
        super(id, version);
        this.sinkName = name;
        this.resource = resource;
        this.description = description == null? "" : description;
    }

    public SinkModel() {
        super(0L, 0L);
        this.sinkName = "";
        this.resource = "";
        this.description = "";
    }

    /**
     * @return resourceName
     */
    public String getResourceName() {
        return resource;
    }

    /**
     * Set resource name
     * @param resourceName Resource name
     */
    public void setResourceName(String resourceName) {
        this.resource = resourceName;
    }

    /**
     * @return sinkName;
     */
    public String getSinkName() {
        return sinkName;
    }

    /**
     * Set sink name
     * @param sinkName Sink name
     */
    public void setSinkName(String sinkName) {
        this.sinkName = sinkName;
    }

    /**
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set sink description
     * @param description Sink description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Checks for empty String values
     * @return true if no empty String values were found, otherwise false
     */
    public boolean isInputFieldsEmpty() {
        return sinkName.isEmpty() || resource.isEmpty() || description.isEmpty();
    }

    /**
     * Checks if the sink name contains illegal characters.
     * A-Å, 0-9, - (minus), + (plus), _ (underscore) and space is valid
     * @return a list containing illegal characters found. Empty list if none found.
     */
    public List<String> getDataioPatternMatches() {
        return Format.getDataioPatternMatches(sinkName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SinkModel)) return false;

        SinkModel sinkModel = (SinkModel) o;

        if (sinkName != null ? !sinkName.equals(sinkModel.sinkName) : sinkModel.sinkName != null) return false;
        if (resource != null ? !resource.equals(sinkModel.resource) : sinkModel.resource != null) return false;
        return !(description != null ? !description.equals(sinkModel.description) : sinkModel.description != null);

    }

    @Override
    public int hashCode() {
        int result = sinkName != null ? sinkName.hashCode() : 0;
        result = 31 * result + (resource != null ? resource.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
