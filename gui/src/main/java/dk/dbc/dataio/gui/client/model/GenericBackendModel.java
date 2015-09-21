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

package dk.dbc.dataio.gui.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GenericBackendModel implements IsSerializable {

    protected long id;
    protected long version;

    /**
     * Constructor
     * @param id the id of the generic model
     * @param version the version of the generic model
     */
    protected GenericBackendModel(long id, long version) {
        this.id = id;
        this.version = version;
    }

    /**
     * Constructor with no parameters
     */
    protected GenericBackendModel() {
    }

    /**
     * Get id
     * @return id of the generic model
     */
    public long getId() {
        return id;
    }

    /**
     * Set id
     * @param id of the generic model
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Get version
     * @return version of the generic model
     */
    public long getVersion() {
        return version;
    }

    /**
     * Set version
     * @param version of the generic model
     */
    public void setVersion(long version) {
        this.version = version;
    }
}
