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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PriorityTest {
    @Test
    public void factory() {
        assertThat("LOW", Priority.of(-42), is(Priority.LOW));
        assertThat("LOW max", Priority.of(1), is(Priority.LOW));
        assertThat("NORMAL min", Priority.of(2), is(Priority.NORMAL));
        assertThat("NORMAL max", Priority.of(6), is(Priority.NORMAL));
        assertThat("HIGH min", Priority.of(7), is(Priority.HIGH));
        assertThat("HIGH", Priority.of(42), is(Priority.HIGH));
    }
}