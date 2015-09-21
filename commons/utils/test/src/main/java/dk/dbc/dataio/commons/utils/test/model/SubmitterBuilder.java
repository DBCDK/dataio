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

import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;

public class SubmitterBuilder {
    private long id = 53L;
    private long version = 1L;
    private SubmitterContent content = new SubmitterContentBuilder().build();

    public SubmitterBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public SubmitterBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public SubmitterBuilder setContent(SubmitterContent content) {
        this.content = content;
        return this;
    }

    public Submitter build() {
        return new Submitter(id, version, content);
    }
}