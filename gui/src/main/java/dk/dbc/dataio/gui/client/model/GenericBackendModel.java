package dk.dbc.dataio.gui.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GenericBackendModel implements IsSerializable {

    protected long id;
    protected long version;

    /**
     * Constructor
     *
     * @param id      the id of the generic model
     * @param version the version of the generic model
     */
    protected GenericBackendModel(long id, long version) {
        this.id = id;
        this.version = version;
    }

    /**
     * Constructor with no parameters
     */
    protected GenericBackendModel() {
    }

    /**
     * Get id
     *
     * @return id of the generic model
     */
    public long getId() {
        return id;
    }

    /**
     * Set id
     *
     * @param id of the generic model
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Get version
     *
     * @return version of the generic model
     */
    public long getVersion() {
        return version;
    }

    /**
     * Set version
     *
     * @param version of the generic model
     */
    public void setVersion(long version) {
        this.version = version;
    }
}
