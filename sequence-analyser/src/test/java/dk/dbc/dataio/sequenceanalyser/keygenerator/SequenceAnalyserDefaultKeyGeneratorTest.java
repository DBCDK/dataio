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

package dk.dbc.dataio.sequenceanalyser.keygenerator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class SequenceAnalyserDefaultKeyGeneratorTest {
    private final long submitter = 42;
    private final SequenceAnalyserDefaultKeyGenerator keyGenerator = new SequenceAnalyserDefaultKeyGenerator(submitter);

    @Test
    public void generateKeys_nullInput_returnsEmptyKeySet() {
        final Set<String> keys = keyGenerator.generateKeys(null);
        assertThat("keys", keys, is(notNullValue()));
        assertThat("keys.size", keys.size(), is(0));
    }

    @Test
    public void generateKeys_emptyInputList_returnsEmptyKeySet() {
        final Set<String> keys = keyGenerator.generateKeys(new ArrayList<>());
        assertThat("keys", keys, is(notNullValue()));
        assertThat("keys.size", keys.size(), is(0));
    }

    @Test
    public void generateKeys_listContainingDuplicateKeys_returnsKeySetWithUniqueValues() {
        final Set<String> keys = keyGenerator.generateKeys(Arrays.asList("id", "parent", "id"));
        assertThat("keys", keys, is(notNullValue()));
        assertThat("keys.size", keys.size(), is(2));
        assertThat("keys contains", keys, contains("id:42", "parent:42"));
    }
}
