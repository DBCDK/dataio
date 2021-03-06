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

package dk.dbc.dataio.commons.types.exceptions;

public class DataIoException extends Exception {
    private static final long serialVersionUID = 6206662806478137024L;

    private final String type;

    /* Below fields are hacks since jackson serialization/deserialization of Throwable
       does not retain type information. Perhaps this can be remedied by configuring
       the ObjectMapper as shown, but this will have profound consequences on the rest
       of the project.

       objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
       objectMapper.setVisibility( PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY );
       objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
     */
    private String causedBy;
    private String causedByDetail;

    public DataIoException() {
        super();
        type = this.getClass().getName();
    }

    /**
     * Constructs a new exception with the specified detail message
     *
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message detail message saved for later retrieval by the
     *                {@link #getMessage()} method. May be null.
     */
    public DataIoException(String message) {
        super(message);
        type = this.getClass().getName();
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause
     *
     * Note that the detail message associated with cause is not
     * automatically incorporated in this exception's detail message.
     *
     * @param  message detail message saved for later retrieval
     *                 by the {@link #getMessage()} method). May be null.
     * @param  cause cause saved for later retrieval by the
     *               {@link #getCause()} method). (A null value is
     *               permitted, and indicates that the cause is nonexistent or
     *               unknown).
     */
    public DataIoException(String message, Exception cause) {
        super(message, cause);
        type = this.getClass().getName();
        causedBy = cause.getClass().getName();
        causedByDetail = cause.getMessage();
    }

    public String getType() {
        return type;
    }

    public String getCausedBy() {
        return causedBy;
    }

    public String getCausedByDetail() {
        return causedByDetail;
    }
}
