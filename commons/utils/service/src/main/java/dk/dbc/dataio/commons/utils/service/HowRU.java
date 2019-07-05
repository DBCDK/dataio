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

package dk.dbc.dataio.commons.utils.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HowRU {
    private static final JSONBContext jsonbContext = new JSONBContext();
    private boolean ok = true;
    private String errorText;
    private Error error;

    public boolean isOk() {
        return ok;
    }

    public HowRU withOk(boolean ok) {
        this.ok = ok;
        return this;
    }

    public String getErrorText() {
        return errorText;
    }

    public HowRU withErrorText(String errorText) {
        this.errorText = errorText;
        return this;
    }

    public Error getError() {
        return error;
    }

    public HowRU setError(Error error) {
        this.error = error;
        return this;
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public HowRU withException(Exception e) {
        ok = false;
        errorText = e.getMessage();
        try {
            error = new Error()
                    .withMessage(e.getMessage())
                    .withStacktrace(StringUtil.getStackTraceString(e));
        } catch (RuntimeException runtimeException) {
            // Unable to serialize stacktrace
        }
        return this;
    }

    public String toJson() {
        try {
            return jsonbContext.marshall(this);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Error {
        private String message;
        private String stacktrace;

        public String getMessage() {
            return message;
        }

        public Error withMessage(String message) {
            this.message = message;
            return this;
        }

        public String getStacktrace() {
            return stacktrace;
        }

        public Error withStacktrace(String stacktrace) {
            this.stacktrace = stacktrace;
            return this;
        }
    }
}
