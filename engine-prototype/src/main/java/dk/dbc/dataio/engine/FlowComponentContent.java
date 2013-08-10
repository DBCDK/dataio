package dk.dbc.dataio.engine;

import java.io.Serializable;
import java.util.List;

/**
 * FlowComponentContent DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class FlowComponentContent implements Serializable {
    private static final long serialVersionUID = -290854497828809813L;

    private /* final */ List<JavaScript> javascripts;
    private /* final */ String invocationMethod;

    private FlowComponentContent() { }

    public FlowComponentContent(List<JavaScript> javascripts, String invocationMethod) {
        this.javascripts = javascripts;
        this.invocationMethod = invocationMethod;
    }

    public String getInvocationMethod() {
        return invocationMethod;
    }

    public List<JavaScript> getJavascripts() {
        return javascripts;
    }
}
