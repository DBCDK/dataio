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

package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.Priority;

public class SubmitterContentJsonBuilder extends JsonBuilder {
    private Long number = 42L;
    private String name = "name";
    private String description = "description";
    private Priority priority = Priority.NORMAL;

    public SubmitterContentJsonBuilder setNumber(Long number) {
        this.number = number;
        return this;
    }

    public SubmitterContentJsonBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public SubmitterContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SubmitterContentJsonBuilder setPriority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asLongMember("number", number)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("description", description)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("priority", priority.name()));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
