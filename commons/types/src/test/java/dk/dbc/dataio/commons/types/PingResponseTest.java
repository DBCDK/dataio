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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * PingResponse unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class PingResponseTest {
    private static final PingResponse.Status STATUS = PingResponse.Status.OK;
    private static final List<String> LOG = Collections.singletonList("message");

    @Test(expected = NullPointerException.class)
    public void constructor_statusArgIsNull_throws() {
        new PingResponse(null, LOG);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_logArgIsNull_throws() {
        new PingResponse(STATUS, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final PingResponse instance = new PingResponse(STATUS, LOG);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_logArgIsEmpty_returnsNewInstance() {
        final PingResponse instance = new PingResponse(STATUS, new ArrayList<String>(0));
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void verify_defensiveCopyingOfLogList() {
        final List<String> log = new ArrayList<>();
        log.add("msg1");
        final PingResponse instance = new PingResponse(STATUS, log);
        assertThat(instance.getLog().size(), is(1));
        log.add("msg2");
        final List<String> returnedLog = instance.getLog();
        assertThat(returnedLog.size(), is(1));
        returnedLog.add("msg3");
        assertThat(instance.getLog().size(), is(1));
    }

    public static PingResponse newPingResponse() {
        return new PingResponse(STATUS, LOG);
    }
}
