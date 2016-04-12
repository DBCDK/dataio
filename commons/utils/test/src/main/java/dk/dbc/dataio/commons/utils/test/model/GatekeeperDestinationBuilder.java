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

import dk.dbc.dataio.commons.types.GatekeeperDestination;

public class GatekeeperDestinationBuilder {

    private long id = 42L;
    private String submitterNumber = "123456";
    private String destination = "destination";
    private String packaging = "packaging";
    private String format = "format";
    private boolean copyToPosthus = false;
    private boolean notifyFromPosthus = false;

    public GatekeeperDestinationBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public GatekeeperDestinationBuilder setSubmitterNumber(String submitterNumber) {
        this.submitterNumber = submitterNumber;
        return this;
    }

    public GatekeeperDestinationBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public GatekeeperDestinationBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public GatekeeperDestinationBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public GatekeeperDestinationBuilder setCopyToPosthus(boolean copyToPosthus) {
        this.copyToPosthus = copyToPosthus;
        return this;
    }

    public GatekeeperDestinationBuilder setNotifyFromPosthus(boolean notifyFromPosthus) {
        this.notifyFromPosthus = notifyFromPosthus;
        return this;
    }


    public GatekeeperDestination build() {
        return new GatekeeperDestination(id, submitterNumber, destination, packaging, format, copyToPosthus, notifyFromPosthus);
    }
}
