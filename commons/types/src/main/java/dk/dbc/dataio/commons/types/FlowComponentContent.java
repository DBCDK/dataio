package dk.dbc.dataio.commons.types;

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

    public FlowComponentContent(String name, List<JavaScript> javascripts, String invocationMethod) {
        this.name = name;
        this.javascripts = new ArrayList<JavaScript>(javascripts);
        this.invocationMethod = invocationMethod;
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
