package dk.dbc.dataio.gui.client.model;

public class SinkModel extends GenericBackendModel {

    private String sinkName;
    private String resource;

    public SinkModel(long id, long version, String name, String resource) {
        super(id, version);
        this.sinkName = name;
        this.resource = resource;
    }

    public SinkModel() {
        super(0L, 0L);
        this.sinkName = "";
        this.resource = "";
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
     * Checks for empty String values
     */
    public boolean isInputFieldsEmpty() {
        return sinkName.isEmpty() || resource.isEmpty();
    }

}
