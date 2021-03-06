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

package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.FlowStoreError;

public class FlowStoreServiceConnectorUnexpectedStatusCodeException extends FlowStoreServiceConnectorException{

    private static final long serialVersionUID = -6137268696536418984L;
    private FlowStoreError flowStoreError;
    private int statusCode;

    /**
     * Constructs a new exception with the specified detail message
     * <p>
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     *
     * @param statusCode the http statusCode code returned by the REST service
     */
    public FlowStoreServiceConnectorUnexpectedStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * @return the statusCode code
     */
    public int getStatusCode(){
        return statusCode;
    }

    /**
     * @return the flow store error
     */
    public FlowStoreError getFlowStoreError() {
        return flowStoreError;
    }

    /**
     * Sets the job error
     * @param flowStoreError the flow store error to set
     */
    public void setFlowStoreError(FlowStoreError flowStoreError) {
        this.flowStoreError = flowStoreError;
    }
}
