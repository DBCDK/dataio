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
 * FlowComponent unit tests
 * The test methods of this class uses the following naming convention:
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowComponentTest {
    private static final long ID = 42L;
    private static final long VERSION = 1L;
    private static final FlowComponentContent CONTENT = FlowComponentContentTest.newFlowComponentContentInstance();
    private static final FlowComponentContent NEXT = FlowComponentContentTest.newFlowComponentContentInstance();

    @Test(expected = NullPointerException.class)
    public void constructor_contentArgIsNull_throws() {
        new FlowComponent(ID, VERSION, null, NEXT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_idArgIsBelowBound_throws() {
        new FlowComponent(Constants.PERSISTENCE_ID_LOWER_BOUND - 1, VERSION, CONTENT, NEXT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_versionArgIsBelowBound_throws() {
        new FlowComponent(ID, Constants.PERSISTENCE_VERSION_LOWER_BOUND - 1, CONTENT, NEXT);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowComponent instance = new FlowComponent(ID, VERSION, CONTENT, NEXT);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_nextArgCanBeNull_returnsNewInstance() {
        final FlowComponent instance = new FlowComponent(ID, VERSION, CONTENT, FlowComponent.UNDEFINED_NEXT);
        assertThat(instance, is(notNullValue()));
    }

    public static FlowComponent newFlowComponentInstance() {
        return new FlowComponent(ID, VERSION, CONTENT, FlowComponent.UNDEFINED_NEXT);
    }
}
