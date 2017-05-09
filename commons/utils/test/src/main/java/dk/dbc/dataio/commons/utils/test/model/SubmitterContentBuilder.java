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

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.SubmitterContent;

public class SubmitterContentBuilder {
    private Long number = 63L;
    private String name = "name";
    private String description = "description";
    private Priority priority = Priority.NORMAL;
    private boolean enabled = true;

    public SubmitterContentBuilder setNumber(Long number) {
        this.number = number;
        return this;
    }

    public SubmitterContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SubmitterContentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public SubmitterContentBuilder setPriority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public SubmitterContentBuilder setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public SubmitterContent build() {
        return new SubmitterContent(number, name, description, priority, enabled);
    }
}