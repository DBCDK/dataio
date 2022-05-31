package dk.dbc.dataio.gui.client.places;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Abstract GWT Place</p>
 * <p>Allows multiple arguments in the URL, and supplies methods for accessing them</p>
 * <br>
 * <p>The place wraps the following fragment identifier in the URL:</p>
 * <pre>   key1=value1&amp;key2=value2&amp;key3=value3 (etc)</pre>
 * <br>
 * <p>Inspired by:</p>
 * <pre>   https://groups.google.com/d/msg/google-web-toolkit/PzlyZ3Gjazg/ZlJGC2wqNyAJ</pre>
 */
public abstract class AbstractBasePlace extends Place {
    protected CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    public abstract Activity createPresenter(ClientFactory clientFactory);

    public Activity presenter = null;
    public static final String INVERT = "!";

    private String token = "";
    private Map<String, PlaceParameterValue> parameters = new LinkedHashMap<>();

    public static class PlaceParameterValue {
        public PlaceParameterValue(String value) {
            this(false, value);
        }

        public PlaceParameterValue(boolean invert, String value) {
            this.invert = invert;
            this.value = value;
        }

        public boolean isInvert() {
            return invert;
        }

        public void setInvert(boolean invert) {
            this.invert = invert;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        private boolean invert;
        private String value;
    }

    /**
     * Constructor for taking multiple tokens as key/value pairs<br>
     * For backwards compatibility reasons, only non-inverted values are given
     *
     * @param tokens will be parsed to extract the parameters passed as part of token
     */
    protected AbstractBasePlace(String... tokens) {
        parameters = tokens2Map(tokens);
        token = parameters2Token(parameters);
    }

    /**
     * Constructor taking a Token
     *
     * @param token The token to be used
     */
    protected AbstractBasePlace(String token) {
        parameters = token2Parameters(token);
        this.token = parameters2Token(parameters);
    }

    /**
     * <p>Checks whether 'this' is constructed with the required parameters. The
     * list of required parameters must be passed as argument.</p>
     *
     * @param keys List of required parameters
     * @return True if validation is ok, false if not
     */
    @SuppressWarnings("unused")
    public boolean validate(String... keys) {
        boolean validationOk = true;
        for (String key : keys) {
            if (!parameters.containsKey(key)) {
                validationOk = false;
            }
        }
        return validationOk;
    }

    /**
     * Gets a list of all keys stored in the Place
     *
     * @return A list of all keys in the Place
     */
    public Set<String> getKeys() {
        return parameters.keySet();
    }

    /**
     * Tests whether there is a parameter with a given key
     *
     * @param key The key to test
     * @return True if the key is present, false if not
     */
    public boolean hasValue(String key) {
        return parameters.containsKey(key);
    }

    /**
     * Gets the Token, that is wrapped in this Place
     *
     * @return The Token, wrapped in this Place
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the Token, that is wrapped in this Place
     *
     * @param token The Token, wrapped in this Place
     */
    public void setToken(String token) {
        if (token != null && !token.isEmpty()) {
            parameters = token2Parameters(token);
            this.token = parameters2Token(parameters);
        }
    }

    /**
     * Gets all parameters, stored in this Place
     *
     * @return All parameters, stored in this Place as a Map
     */
    public Map<String, String> getParameters() {
        Map<String, String> result = new LinkedHashMap<>();
        parameters.forEach((key, value) -> result.put(key, value.getValue()));
        return result;
    }

    /**
     * Gets all parameters, stored in this Place - including inversions
     *
     * @return All parameters, stored in this Place as a Map
     */
    public Map<String, PlaceParameterValue> getDetailedParameters() {
        return parameters;
    }

    /**
     * Replaces all parameters in this place, with the values supplied
     *
     * @param parameters New parameters to be stored instead of the old values
     */
    public void setParameters(Map<String, PlaceParameterValue> parameters) {
        this.parameters = parameters;
        token = parameters2Token(parameters);
    }

    /**
     * Fetches a named parameter from the list of stored parameters <br>
     * If no parameters exists with the given name, null is returned.
     *
     * @param key The name of the parameter
     * @return The value of the parameter if it exists, null if no matches are found
     */
    public String getParameter(String key) {
        PlaceParameterValue parameterValue = parameters.get(key);
        return parameterValue == null ? null : parameters.get(key).getValue();
    }

    /**
     * Fetches a named parameter from the list of stored parameters - including inversion<br>
     * If no parameters exists with the given name, null is returned.
     *
     * @param key The name of the parameter
     * @return The value of the parameter if it exists, null if no matches are found
     */
    public PlaceParameterValue getDetailedParameter(String key) {
        return parameters.get(key);
    }

    /**
     * Adds a named parameter to the place.
     *
     * @param key   The name of the parameter
     * @param value The value of the parameter
     */
    public void addParameter(String key, String value) {
        parameters.put(key, new PlaceParameterValue(value));
        token = parameters2Token(parameters);
    }

    /**
     * Adds a named parameter to the place with the invert option <br>
     * Invert means, that the notation in the URL is:<br>
     * key!=value
     *
     * @param key    The name of the parameter
     * @param invert True: Invert value, False: Do not invert value
     * @param value  The value of the parameter
     */
    public void addParameter(String key, boolean invert, String value) {
        parameters.put(key, new PlaceParameterValue(invert, value));
        token = parameters2Token(parameters);
    }

    /**
     * Removes a parameter from the place
     *
     * @param key The name of the parameter
     * @return The former value of the parameter indexed by key
     */
    public PlaceParameterValue removeParameter(String key) {
        final PlaceParameterValue formerValue = parameters.remove(key);
        token = parameters2Token(parameters);
        return formerValue;
    }

    /**
     * Maps a list of tokens to a Map (ordered map - LinkedHashMap) <br>
     * The tokens: key1, value1, key2, value2 etc...
     *
     * @param tokens The list of tokens
     * @return The Map containing all the key/value pairs
     */
    public Map<String, PlaceParameterValue> tokens2Map(String... tokens) {
        Map<String, PlaceParameterValue> map = new LinkedHashMap<>();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] == null || tokens[i].isEmpty()) break;
            map.put(tokens[i++], new PlaceParameterValue(i >= tokens.length || tokens[i] == null ? "" : tokens[i]));
        }
        return map;
    }

    /**
     * Maps a Map&lt;String, PlaceParameterValue&gt; to a Token (String) in the form: <br>
     * key1=value1&amp;key2=value2 etc...
     *
     * @param parameters The Map of key/value pairs
     * @return The resulting Token
     */
    public static String parameters2Token(Map<String, PlaceParameterValue> parameters) {
        StringBuilder result = new StringBuilder();
        for (String key : parameters.keySet()) {
            if (result.length() > 0) {
                result.append("&");
            }
            result.append(key);
            PlaceParameterValue value = parameters.get(key);
            if (value != null && value.getValue() != null && !value.getValue().isEmpty()) {
                if (value.isInvert()) {
                    result.append(INVERT);
                }
                result.append("=");
                result.append(parameters.get(key).getValue());
            }
        }
        return result.toString();
    }

    /**
     * Maps a Token in the form key1=value2&amp;key2=value2... to a Map (ordered LinkedHashMap)
     *
     * @param token The Token string to map to a LinkedHashMap
     * @return The resulting Map consisting of key/value pairs from the token
     */
    public static Map<String, PlaceParameterValue> token2Parameters(String token) {
        Map<String, PlaceParameterValue> map = new LinkedHashMap<>();
        if (token == null || token.isEmpty()) return map;
        for (String listItem : Arrays.asList(token.split("&"))) {
            List<String> valuePairs = Arrays.asList(listItem.split("\\" + INVERT + "?=", 2));
            String key = valuePairs.get(0);
            if (valuePairs.size() == 1) {  // Key without value
                map.put(key, new PlaceParameterValue(""));
            } else {  // Both key and value
                map.put(key, new PlaceParameterValue(listItem.startsWith(key + INVERT + "="), valuePairs.get(1)));
            }
        }
        return map;
    }


    /*
     * Protected methods
     */

    /**
     * Gets the presenter, stored in the place
     *
     * @return The presenter
     */
    protected Activity getPresenter() {
        return presenter;
    }

}
