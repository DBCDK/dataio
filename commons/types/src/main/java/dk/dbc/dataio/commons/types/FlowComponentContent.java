package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FlowComponentContent DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class FlowComponentContent implements Serializable {
    private static final long serialVersionUID = -290854497828809813L;

    private /* final */ String name;
    private /* final */ List<JavaScript> javascripts;
    private /* final */ String invocationMethod;

    private FlowComponentContent() { }

    /**
     * Class constructor
     *
     * @param name name of flow component
     * @param javascripts list of attached JavaScripts (can be empty)
     * @param invocationMethod name of invocation method (can be empty)
     *
     * @throws NullPointerException if given null-valued name, javascripts or invocationMethod argument
     * @throws IllegalArgumentException if given empty-valued name argument
     */
    public FlowComponentContent(String name, List<JavaScript> javascripts, String invocationMethod) {
        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.invocationMethod = InvariantUtil.checkNotNullOrThrow(invocationMethod, "invocationMethod");
        this.javascripts = new ArrayList<JavaScript>(InvariantUtil.checkNotNullOrThrow(javascripts, "javascripts"));
    }

    public String getName() {
        return name;
    }

    public String getInvocationMethod() {
        return invocationMethod;
    }

    public List<JavaScript> getJavascripts() {
        return new ArrayList<JavaScript>(javascripts);
    }
}
