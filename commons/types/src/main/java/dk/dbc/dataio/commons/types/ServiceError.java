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

import java.io.Serializable;

public class ServiceError implements Serializable {
    private static final long serialVersionUID = -7949904926077016654L;

    private /* final */ String message;
    private /* final */ String details;
    private /* final */ String stacktrace;

    public ServiceError() { }

    public ServiceError withMessage(String message) {
        this.message = message;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ServiceError withDetails(String details) {
        this.details = details;
        return this;
    }

    public String getDetails() {
        return details;
    }

    public ServiceError withStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
        return this;
    }

    public String getStacktrace() {
        return stacktrace;
    }

}
