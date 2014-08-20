package dk.dbc.dataio.gui.client.model;

public class GenericModel {

    protected Long id;
    protected Long version;

    /**
     * Constructor
     * @param id
     * @param version
     */
    protected GenericModel(Long id, Long version) {
        this.id = id;
        this.version = version;
    }

    /**
     * Constructor with no parameters
     */
    protected GenericModel() {
    }

    /**
     * Get id
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * Set id
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get version
     * @return version
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Set version
     * @param version
     */
    public void setVersion(Long version) {
        this.version = version;
    }
}
