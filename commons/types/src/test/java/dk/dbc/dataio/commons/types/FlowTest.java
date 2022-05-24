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

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Flow unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowTest {
    private static final long ID = 42L;
    private static final long VERSION = 1L;
    private static final FlowContent CONTENT = FlowContentTest.newFlowContentInstance().withTimeOfFlowComponentUpdate(new Date());

    @Test(expected = NullPointerException.class)
    public void constructor_contentArgIsNull_throws() {
        new Flow(ID, VERSION, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_idArgIsLessThanLowerBound_throws() {
        new Flow(Constants.PERSISTENCE_ID_LOWER_BOUND - 1, VERSION, CONTENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_versionArgIsLessThanLowerBound_throws() {
        new Flow(ID, Constants.PERSISTENCE_VERSION_LOWER_BOUND - 1, CONTENT);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final Flow instance = new Flow(ID, VERSION, CONTENT);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void hasNextComponents_flowHasNoComponentsContainingNextEntries_returnsFalse() {
        assertThat(newFlowInstance().hasNextComponents(), is(false));
    }

    public static Flow newFlowInstance() {
        return new Flow(ID, VERSION, CONTENT);
    }
}
