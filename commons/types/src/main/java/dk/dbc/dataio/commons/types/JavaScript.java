package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * JavaScript DTO class.
 */
public class JavaScript implements Serializable {
    private static final long serialVersionUID = -6885710844080239998L;
    private final String javascript;
    private final String moduleName;

    /**
     * Class constructor
     *
     * @param javascript JavaScript source code
     * @param moduleName JavaScript module name (can be empty)
     * @throws NullPointerException     if given null-valued javascript or moduleName argument
     * @throws IllegalArgumentException if given empty-valued javascript or moduleName argument
     */
    @JsonCreator
    public JavaScript(@JsonProperty("javascript") String javascript,
                      @JsonProperty("moduleName") String moduleName) {

        this.javascript = InvariantUtil.checkNotNullNotEmptyOrThrow(javascript, "javascript");
        this.moduleName = InvariantUtil.checkNotNullOrThrow(moduleName, "moduleName");
    }

    public String getJavascript() {
        return javascript;
    }

    public String getModuleName() {
        return moduleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JavaScript)) return false;

        JavaScript that = (JavaScript) o;

        return javascript.equals(that.javascript)
                && moduleName.equals(that.moduleName);
    }

    @Override
    public int hashCode() {
        int result = javascript.hashCode();
        result = 31 * result + moduleName.hashCode();
        return result;
    }
}
