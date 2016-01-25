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

package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.Diagnostic;

public class DiagnosticBuilder {
    private Diagnostic.Level level = Diagnostic.Level.FATAL;
    private String message = "diagnostic message";
    private String stackTrace = null;
    private String tag = null;
    private String attribute = null;

    public DiagnosticBuilder setLevel(Diagnostic.Level level) {
        this.level = level;
        return this;
    }

    public DiagnosticBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public DiagnosticBuilder setStacktrace(String stacktrace) {
        this.stackTrace = stacktrace;
        return this;
    }

    public DiagnosticBuilder setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public DiagnosticBuilder setAttribute(String attribute) {
        this.attribute = attribute;
        return this;
    }

    public Diagnostic build() {
        return new Diagnostic(level, message, stackTrace, tag, attribute);
    }
}
