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

package dk.dbc.dataio.gui.server.jobrerun;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class JobRerunScheme implements Serializable {

    public static final String TICKLE_TOTAL = "dataio/sinks/tickle-repo/total";
    public enum Action { COPY, RERUN_ALL, RERUN_FAILED }
    public enum Type { RR, TICKLE, ORIGINAL_FILE }

    private Set<Action> actions;
    private Type type;

    public JobRerunScheme() {}

    public Type getType() {
        return type;
    }

    public JobRerunScheme withType(Type type) {
        this.type = type;
        return this;
    }

    public Set<Action> getActions() {
        return actions;
    }

    public JobRerunScheme withActions(Set<Action> actions) {
        this.actions = new HashSet<>(actions);
        return this;
    }

    @Override
    public String toString() {
        return "JobRerunScheme{" +
                "actions=" + actions +
                ", type=" + type +
                '}';
    }
}