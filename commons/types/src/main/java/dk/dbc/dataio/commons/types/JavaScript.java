package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

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

    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param javascript JavaScript source code
     * @param moduleName JavaScript module name (can be empty)
     *
     * @throws NullPointerException if given null-valued javascript or moduleName argument
     * @throws IllegalArgumentException if given empty-valued javascript or moduleName argument
     */
    public JavaScript(String javascript, String moduleName) {
        this.javascript = InvariantUtil.checkNotNullNotEmptyOrThrow(javascript, "javascript");
        this.moduleName = InvariantUtil.checkNotNullOrThrow(moduleName, "moduleName");
    }

    public String getJavascript() {
        return javascript;
    }

    public String getModuleName() {
        return moduleName;
    }
}
