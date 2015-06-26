package dk.dbc.dataio.commons.utils.httpclient;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds path from given path template interpolating set variables as needed
 */
public class PathBuilder {
    public static final String PATH_SEPARATOR = "/";

    private final String pathTemplate;
    private final Map<String, String> variables;

    /**
     * Class constructor
     * @param pathTemplate path template
     * @throws NullPointerException if given null-valued argument
     */
    public PathBuilder(String pathTemplate) throws NullPointerException {
        this.pathTemplate = InvariantUtil.checkNotNullOrThrow(pathTemplate, "pathTemplate");
        variables = new HashMap<>();
    }

    /**
     * Binds path variable to value
     *
     * @param variableName the path variable
     * @param value the value
     * @return this path builder
     */
    public PathBuilder bind(String variableName, String value) {
        variables.put(variableName, value);
        return this;
    }

    /**
     * Binds path variable to value
     *
     * @param variableName the path variable
     * @param value the value
     * @return this path builder
     */
    public PathBuilder bind(String variableName, long value) {
        variables.put(variableName, Long.toString(value));
        return this;
    }

    /**
     * Builds path
     * @return path as separate path elements
     */
    public String[] build() {
        String path = pathTemplate;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            path = path.replaceAll(String.format("\\{%s\\}", entry.getKey()), entry.getValue());
        }
        return path.split(PATH_SEPARATOR);
    }
}
