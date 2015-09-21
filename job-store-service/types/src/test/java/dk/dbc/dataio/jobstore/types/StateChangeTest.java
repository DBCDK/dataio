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

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class StateChangeTest {

    @Test
    public void constructor_noArgs_returnsNewInstanceWithDefaultValues() {
        StateChange stateChange = new StateChange();
        assertNewStateChange(stateChange);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setSucceeded_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        stateChange.setSucceeded(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setFailed_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        stateChange.setFailed(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setIgnored_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        stateChange.setIgnored(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void incSucceeded_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        stateChange.incSucceeded(-1);
    }

    @Test
    public void incSucceeded_increments() {
        StateChange stateChange = new StateChange();
        stateChange.incSucceeded(1);
        assertThat(stateChange.getSucceeded(), is(1));
        stateChange.incSucceeded(9);
        assertThat(stateChange.getSucceeded(), is(10));
    }

    @Test(expected = IllegalArgumentException.class)
    public void incFailed_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        stateChange.incFailed(-1);
    }

    @Test
    public void incFailed_increments() {
        StateChange stateChange = new StateChange();
        stateChange.incFailed(1);
        assertThat(stateChange.getFailed(), is(1));
        stateChange.incFailed(9);
        assertThat(stateChange.getFailed(), is(10));
    }

    @Test(expected = IllegalArgumentException.class)
    public void incIgnored_lessThanZeroNotAllowed_throws() {
        StateChange stateChange = new StateChange();
        stateChange.incIgnored(-1);
    }

    @Test
    public void incIgnored_increments() {
        StateChange stateChange = new StateChange();
        stateChange.incIgnored(1);
        assertThat(stateChange.getIgnored(), is(1));
        stateChange.incIgnored(9);
        assertThat(stateChange.getIgnored(), is(10));
    }

    @Test
    public void setAllValues_allValuesAreSet_valuesAreCorrect() {
        final Date DATE = new Date(System.currentTimeMillis());
        StateChange stateChange = new StateChange();
        stateChange.setBeginDate(DATE);
        stateChange.setEndDate(DATE);
        stateChange.setSucceeded(10);
        stateChange.setFailed(2);
        stateChange.setIgnored(3);
        stateChange.setPhase(State.Phase.PROCESSING);

        assertThat(stateChange.getBeginDate(), is(DATE));
        assertThat(stateChange.getEndDate(), is(DATE));
        assertThat(stateChange.getSucceeded(), is(10));
        assertThat(stateChange.getFailed(), is(2));
        assertThat(stateChange.getIgnored(), is(3));
        assertThat(stateChange.getPhase(), is(State.Phase.PROCESSING));
    }

    /*
     * Private methods
     */

    private void assertNewStateChange(StateChange stateChange) {
        assertThat(stateChange.getBeginDate(), is(nullValue()));
        assertThat(stateChange.getEndDate(), is(nullValue()));
        assertThat(stateChange.getSucceeded(), is(0));
        assertThat(stateChange.getFailed(), is(0));
        assertThat(stateChange.getIgnored(), is(0));
        assertThat(stateChange.getPhase(), is(nullValue()));
    }
}
