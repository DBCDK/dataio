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
     *
     * @throws NullPointerException if given null-valued javascript or moduleName argument
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
