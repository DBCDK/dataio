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

package dk.dbc.dataio.jobstore.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class StateElementTest {

    @Test
    public void constructor_noArgs_returnsNewInstanceWithInitializedStateElements() {
        StateElement stateElement = new StateElement();
        assertStateElement(stateElement);
    }

    @Test
    public void deepCopyConstructor_stateElementArg_returnsNewInstanceWithCopiedValues() {
        StateElement stateElement = getStateElement();
        StateElement stateElementDeepCopy = new StateElement(stateElement);
        assertThat(stateElementDeepCopy, is(stateElement));
    }

    /*
     * Private methods
     */

    private void assertStateElement(StateElement stateElement) {
        assertThat(stateElement.getBeginDate(), is(nullValue()));
        assertThat(stateElement.getEndDate(), is(nullValue()));
        assertThat(stateElement.getSucceeded(), is(0));
        assertThat(stateElement.getFailed(), is(0));
        assertThat(stateElement.getIgnored(), is(0));
    }

    private StateElement getStateElement() {
        StateElement stateElement = new StateElement();
        stateElement.setSucceeded(9);
        stateElement.setIgnored(1);
        return stateElement;
    }
}
