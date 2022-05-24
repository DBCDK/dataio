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
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * FlowBinder unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowBinderTest {
    private static final long ID = 42L;
    private static final long VERSION = 1L;
    private static final FlowBinderContent CONTENT = FlowBinderContentTest.newFlowBinderContentInstance();

    @Test(expected = NullPointerException.class)
    public void constructor_contentArgIsNull_throws() {
        new FlowBinder(ID, VERSION, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_idArgIsLessThanLowerBound_throws() {
        new FlowBinder(Constants.PERSISTENCE_ID_LOWER_BOUND - 1, VERSION, CONTENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_versionArgIsLessThanLowerBound_throws() {
        new FlowBinder(ID, Constants.PERSISTENCE_VERSION_LOWER_BOUND - 1, CONTENT);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowBinder instance = new FlowBinder(ID, VERSION, CONTENT);
        assertThat(instance, is(notNullValue()));
    }

    public static FlowBinder newFlowBinderInstance() {
        return new FlowBinder(ID, VERSION, CONTENT);
    }
}
