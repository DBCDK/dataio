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

package dk.dbc.dataio.commons.javascript;

import java.io.Serializable;

public class JavaScriptProjectException extends Exception implements Serializable {
    private static final long serialVersionUID = 6907428727283491685L;
    private /* final */ JavaScriptProjectError errorCode;

    public JavaScriptProjectException() {
        this.errorCode = JavaScriptProjectError.UNKNOWN;
    }

    public JavaScriptProjectException(final JavaScriptProjectError errorCode, final Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public JavaScriptProjectException(final JavaScriptProjectError errorCode, final String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
    }

    public JavaScriptProjectError getErrorCode() {
        return errorCode;
    }
}