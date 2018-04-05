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
 * GatekeeperDestination DTO class.
 */
public class GatekeeperDestination implements Serializable {

    private static final long serialVersionUID = -4446705379590875306L;
    private /*final*/ long id;
    private /*final*/ String submitterNumber;
    private /*final*/ String destination;
    private /*final*/ String packaging;
    private /*final*/ String format;
    private /*final*/ boolean copyToPosthus;
    private /*final*/ boolean notifyFromPosthus;

    @JsonCreator
    public GatekeeperDestination(@JsonProperty("id") long id,
                                 @JsonProperty("submitterNumber") String submitterNumber,
                                 @JsonProperty("destination") String destination,
                                 @JsonProperty("packaging") String packaging,
                                 @JsonProperty("format") String format,
                                 @JsonProperty("copyToPosthus") boolean copyToPosthus,
                                 @JsonProperty("notifyFromPosthus") boolean notifyFromPosthus) {

        this.id = id;
        this.submitterNumber = InvariantUtil.checkNotNullNotEmptyOrThrow(submitterNumber, "submitterNumber");
        this.destination = InvariantUtil.checkNotNullNotEmptyOrThrow(destination, "destination");
        this.packaging = InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        this.format = InvariantUtil.checkNotNullNotEmptyOrThrow(format, "format");
        this.copyToPosthus = copyToPosthus;
        this.notifyFromPosthus = notifyFromPosthus;
    }

    //This constructor only exists due to gwt serialization issues.
    private GatekeeperDestination() { }

    public long getId() {
        return id;
    }

    public String getSubmitterNumber() {
        return submitterNumber;
    }

    public String getDestination() {
        return destination;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getFormat() {
        return format;
    }

    public boolean isCopyToPosthus() {
        return copyToPosthus;
    }

    public boolean isNotifyFromPosthus() {
        return notifyFromPosthus;
    }

    public GatekeeperDestination withId(long id) {
        this.id = id;
        return this;
    }

    public GatekeeperDestination withSubmitterNumber(String submitterNumber) throws IllegalArgumentException {
        this.submitterNumber = InvariantUtil.checkNotEmptyOrThrow(submitterNumber, "submitterNumber");
        return this;
    }

    public GatekeeperDestination withDestination(String destination) throws IllegalArgumentException {
        this.destination = InvariantUtil.checkNotNullNotEmptyOrThrow(destination, "destination");
        return this;
    }

    public GatekeeperDestination withPackaging(String packaging) throws IllegalArgumentException {
        this.packaging = InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        return this;
    }

    public GatekeeperDestination withFormat(String format) throws IllegalArgumentException {
        this.format = InvariantUtil.checkNotNullNotEmptyOrThrow(format, "format");
        return this;
    }

    public GatekeeperDestination withCopyToPosthus(boolean copyToPosthus) {
        this.copyToPosthus = copyToPosthus;
        return this;
    }

    public GatekeeperDestination withNotifyFromPosthus(boolean notifyFromPosthus) {
        this.notifyFromPosthus = notifyFromPosthus;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GatekeeperDestination)) return false;

        GatekeeperDestination that = (GatekeeperDestination) o;

        return submitterNumber.equals(that.submitterNumber)
                && destination.equals(that.destination)
                && packaging.equals(that.packaging)
                && format.equals(that.format);
    }

    @Override
    public int hashCode() {
        int result = submitterNumber.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + packaging.hashCode();
        result = 31 * result + format.hashCode();
        return result;
    }
}
