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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * GatekeeperDestination unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class GatekeeperDestinationTest {
    private static final long ID = 42L;
    private static final String SUBMITTER_NUMBER = "123456";
    private static final String DESTINATION = "destination";
    private static final String PACKAGING = "lin";
    private static final String FORMAT = "marc2";
    private static final boolean COPY_TO_POSTHUS = false;
    private static final boolean NOTIFY_FROM_POSTHUS = false;

    @Test(expected = NullPointerException.class)
    public void constructor_submitterNumberArgIsNull_throws() {
        new GatekeeperDestination(ID, null, DESTINATION, PACKAGING, FORMAT, COPY_TO_POSTHUS, NOTIFY_FROM_POSTHUS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_submitterNumberArgIsEmpty_throws() {
        new GatekeeperDestination(ID, "", DESTINATION, PACKAGING, FORMAT, COPY_TO_POSTHUS, NOTIFY_FROM_POSTHUS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_destinationArgIsNull_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, null, PACKAGING, FORMAT, COPY_TO_POSTHUS, NOTIFY_FROM_POSTHUS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_destinationArgIsEmpty_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, "", PACKAGING, FORMAT, COPY_TO_POSTHUS, NOTIFY_FROM_POSTHUS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_packagingArgIsNull_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, null, FORMAT, COPY_TO_POSTHUS, NOTIFY_FROM_POSTHUS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_packagingArgIsEmpty_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, "", FORMAT, COPY_TO_POSTHUS, NOTIFY_FROM_POSTHUS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_formatArgIsNull_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, PACKAGING, null, COPY_TO_POSTHUS, NOTIFY_FROM_POSTHUS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_formatArgIsEmpty_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, PACKAGING, "", COPY_TO_POSTHUS, NOTIFY_FROM_POSTHUS);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final GatekeeperDestination instance = new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, PACKAGING, FORMAT, COPY_TO_POSTHUS, NOTIFY_FROM_POSTHUS);
        assertThat(instance, is(notNullValue()));
    }

    public static GatekeeperDestination newGatekeeperDestinationInstance() {
        return new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, PACKAGING, FORMAT, COPY_TO_POSTHUS, NOTIFY_FROM_POSTHUS);
    }
}
