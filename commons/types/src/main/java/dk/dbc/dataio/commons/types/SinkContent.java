package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * SinkContent DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class SinkContent implements Serializable {
    private static final long serialVersionUID = -3413557101203220951L;

    private /* final */ String name;
    private /* final */ String resource;

    private SinkContent() { }

    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param name sink name
     * @param resource sink resource
     *
     * @throws NullPointerException if given null-valued name or resource argument
     * @throws IllegalArgumentException if given empty-valued name or resource argument
     */
    public SinkContent(String name, String resource) {
        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.resource = InvariantUtil.checkNotNullNotEmptyOrThrow(resource, "resource");
    }

    public String getName() {
        return name;
    }

    public String getResource() {
        return resource;
    }
}
