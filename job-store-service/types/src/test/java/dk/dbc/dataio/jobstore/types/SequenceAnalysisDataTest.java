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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class SequenceAnalysisDataTest {
    @Test(expected = NullPointerException.class)
    public void constructor_dataArgIsNull_throws() {
        new SequenceAnalysisData(null);
    }

    @Test
    public void constructor_dataArgIsNonEmpty_returnsNewInstance() {
        final Set<String> expectedData = new HashSet(Arrays.asList("first", "second"));
        final SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(expectedData);
        assertThat(sequenceAnalysisData, is(notNullValue()));
        assertThat(sequenceAnalysisData.getData(), is(expectedData));
    }

    @Test
    public void constructor_dataArgIsEmpty_returnsNewInstance() {
        final Set<String> expectedData = Collections.emptySet();
        final SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(expectedData);
        assertThat(sequenceAnalysisData, is(notNullValue()));
        assertThat(sequenceAnalysisData.getData(), is(expectedData));
    }

    @Test
    public void getData_returnedListIsUnmodifiable() {
        final Set<String> expectedData = new HashSet(Arrays.asList("first", "second"));
        final SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(expectedData);
        try {
            sequenceAnalysisData.getData().add("third");
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {}
    }
}
