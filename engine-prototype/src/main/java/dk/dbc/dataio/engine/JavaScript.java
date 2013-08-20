package dk.dbc.dataio.engine;

import java.io.Serializable;

/**
 * JavaScript DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class JavaScript implements Serializable {
    private static final long serialVersionUID = -6885710844080239998L;
    private /* final */ String javascript;
    private /* final */ String moduleName;

    private JavaScript() { }

    public JavaScript(String javascript, String moduleName) {
        this.javascript = javascript;
        this.moduleName = moduleName;
    }

    public String getJavascript() {
        return javascript;
    }

    public String getModuleName() {
        return moduleName;
    }
}
