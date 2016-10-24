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
 * <p>The place wraps the following URL:</p>
 * <pre>   key1=value1&amp;key2=value2&amp;key3=value3 (etc)</pre>
 * <br>
 * <p>Keys are converted to lowercase, when entering the Place</p>
 * <br>
 * <p>Inspired by:</p>
 * <pre>   https://groups.google.com/d/msg/google-web-toolkit/PzlyZ3Gjazg/ZlJGC2wqNyAJ</pre>
 */
public abstract class AbstractBasePlace extends Place {
    protected CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    public abstract Activity createPresenter(ClientFactory clientFactory);

    private static final String VALIDATE_TOKENS_MESSAGE_PREFIX = "Missing token(s): ";

    private String url = "";
    private Map<String, String> parameters = new LinkedHashMap<>();

    /**
     * Constructor for taking multiple tokens as key/value pairs
     *
     * @param tokens will be parsed to extract the parameters passed as part of url
     */
    protected AbstractBasePlace(String... tokens) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] == null || tokens[i].isEmpty()) break;
            final String key = tokens[i++].toLowerCase();
            final String value = i>=tokens.length || tokens[i]==null ? "" : tokens[i];
            // build parameters map
            parameters.put(key, value);
            // build url
            if (i >= 2) {
                result.append("&");
            }
            result.append(key);
            if (!value.isEmpty()) {
                result.append("=").append(value);
            }
        }
        url = result.toString();
    }

    /**
     * Constructor taking an URL
     *
     * @param url The url to be used
     */
    protected AbstractBasePlace(String url) {
        if (url == null || url.isEmpty()) {
            GWT.log("AbstractBasePlace: Url is empty, no-op");
            return;
        }
        List<String> list = Arrays.asList(url.split("&"));
        for (String listItem : list) {
            List<String> valuePairs = Arrays.asList(listItem.split("=", 2));
            if (valuePairs.size() == 0) {
                GWT.log("AbstractBasePlace: Invalid parameters");
            } else if (valuePairs.size() == 1) {  // Key without value
                parameters.put(valuePairs.get(0).toLowerCase(), "");
            } else {  // Both key and value
                parameters.put(valuePairs.get(0).toLowerCase(), valuePairs.get(1));
            }
        }
        StringBuilder result = new StringBuilder();
        for (String key: parameters.keySet()) {
            if (!result.toString().isEmpty()) {
                result.append("&");
            }
            result.append(key);
            if (!parameters.get(key).isEmpty()) {
                result.append("=").append(parameters.get(key));
            }
        }
        this.url = result.toString();
    }

    /**
     * <p>Checks whether 'this' is constructed with the required parameters. The
     * list of required parameters must be passed as argument.</p>
     * <p>If any tokens are missing, they are logged with GWT.log</p>
     *
     * @param keys List of required parameters
     * @return True if validation is ok, false if not
     */
    public boolean validate(String... keys) {
        boolean validationOk = true;
        StringBuilder message = new StringBuilder(VALIDATE_TOKENS_MESSAGE_PREFIX);
        for (String key: keys) {
            if (!parameters.containsKey(key)) {
                message.append(key).append(" ");
                validationOk = false;
            }
        }
        // if something is added to the string show
        if (!message.toString().equals(VALIDATE_TOKENS_MESSAGE_PREFIX)) {
            GWT.log("AbstractBasePlace: " + message.toString());
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
     * Gets the URL, that is wrapped in this Place
     *
     * @return The Url, wrapped in this Place
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the value, associated with a given key
     *
     * @param key The Key for the parameter to get
     * @return The value
     */
    public String getValue(String key) {
        return parameters.get(key);
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
     * Gets all parameters, stored in this Place
     *
     * @return All parameters, stored in this Place as a Map
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

}