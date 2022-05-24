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

package dk.dbc.dataio.harvester.corepo;

import dk.dbc.dataio.commons.types.Pid;
import org.junit.Test;

import java.util.Collections;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PidFilterTest {

    @Test
    public void constructor_inputIsnull_ok() {
        assertThat(() -> new PidFilter(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void test_returns() {
        PidFilter pidFilter = new PidFilter(Collections.singleton(870970));
        assertThat(pidFilter.test(Pid.of("870971-basis:23142546")), is(false));
        assertThat(pidFilter.test(Pid.of("unit:1354373")), is(false));
        assertThat(pidFilter.test(Pid.of("870970-basis:23142546")), is(true));
    }
}
