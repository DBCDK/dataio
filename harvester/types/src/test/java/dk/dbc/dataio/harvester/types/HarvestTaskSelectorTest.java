/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.harvester.types;

import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HarvestTaskSelectorTest {
    @Test
    public void stringRepresentation() {
        assertThat(new HarvestTaskSelector("key", "val").toString(), is("key = val"));
        assertThat(new HarvestTaskSelector("key", null).toString(), is("key = null"));
        assertThat(new HarvestTaskSelector(null, "val").toString(), is("null = val"));
    }

    @Test
    public void of() {
        assertThat("key=val", HarvestTaskSelector.of("key=val").toString(), is("key = val"));
        assertThat("  key  =  val  ", HarvestTaskSelector.of("  key  =  val  ").toString(), is("key = val"));
        assertThat("key=val1=val2", HarvestTaskSelector.of("key=val1=val2").toString(), is("key = val1=val2"));
        assertThat("no value", () -> HarvestTaskSelector.of("key"), isThrowing(IllegalArgumentException.class));
        assertThat("no equals", () -> HarvestTaskSelector.of("key : val"), isThrowing(IllegalArgumentException.class));
    }
}