/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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

    private String token = "";
    private Map<String, String> parameters = new LinkedHashMap<>();

    /**
     * Constructor for taking multiple tokens as key/value pairs
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
        for (String key: keys) {
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
        return parameters;
    }

    /**
     * Replaces all parameters in this place, with the values supplied
     *
     * @param parameters New parameters to be stored instead of the old values
     */
    public void setParameters(Map<String, String> parameters) {
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
        return parameters.get(key);
    }

    /**
     * Adds a named parameter to the place.
     *
     * @param key The name of the parameter
     * @param value The value of the parameter
     */
    public void addParameter(String key, String value) {
        parameters.put(key, value);
        token = parameters2Token(parameters);
    }

    /**
     * Removes a parameter from the place
     *
     * @param key The name of the parameter
     * @return The former value of the parameter indexed by key
     */
    public String removeParameter(String key) {
        final String formerValue = parameters.remove(key);
        token = parameters2Token(parameters);
        return formerValue;
    }


    /*
     * Protected methods
     */

    /**
     * Gets the presenter, stored in the place
     * @return The presenter
     */
    protected Activity getPresenter() {
        return presenter;
    }


    /*
     * Public static methods
     */

    /**
     * Maps a list of tokens to a Map (ordered map - LinkedHashMap) <br>
     * The tokens: key1, value1, key2, value2 etc...
     *
     * @param tokens The list of tokens
     * @return The Map containing all the key/value pairs
     */
    public static Map<String, String> tokens2Map(String... tokens) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] == null || tokens[i].isEmpty()) break;
            map.put(tokens[i++], i>=tokens.length || tokens[i]==null ? "" : tokens[i]);
        }
        return map;
    }

    /**
     * Maps a Map&lt;String, String&gt; to an Token (String) in the form: <br>
     *     key1=value1&amp;key2=value2 etc...
     *
     * @param parameters The Map of key/value pairs
     * @return The resulting Token
     */
    public static String parameters2Token(Map<String, String> parameters) {
        StringBuilder result = new StringBuilder();
        for (String key: parameters.keySet()) {
            if (result.length() > 0) {
                result.append("&");
            }
            result.append(key);
            String value = parameters.get(key);
            if (value != null && !parameters.get(key).isEmpty()) {
                result.append("=").append(parameters.get(key));
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
    public static Map<String, String> token2Parameters(String token) {
        Map<String, String> map = new LinkedHashMap<>();
        if (token == null || token.isEmpty()) return map;
        for (String listItem : Arrays.asList(token.split("&"))) {
            List<String> valuePairs = Arrays.asList(listItem.split("=", 2));
            if (valuePairs.size() == 1) {  // Key without value
                map.put(valuePairs.get(0), "");
            } else {  // Both key and value
                map.put(valuePairs.get(0), valuePairs.get(1));
            }
        }
        return map;
    }

}