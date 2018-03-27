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

package dk.dbc.httpclient;

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
