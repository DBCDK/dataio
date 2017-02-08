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

package dk.dbc.dataio.sink.worldcat;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UnicodeNormalizationFormDecomposedTest {
    @Test
    public void normalizations() {
        assertThat("ø", getUnicodeValuesOfString(UnicodeNormalizationFormDecomposed.of("ø")), is("\\u00f8"));
        assertThat("ä", getUnicodeValuesOfString(UnicodeNormalizationFormDecomposed.of("ä")), is("\\u0061\\u0308"));
    }

    private String getUnicodeValuesOfString(String str) {
        final StringBuilder buffer = new StringBuilder();
        for (char c : str.toCharArray()) {
            buffer.append(String.format("\\u%04x", (int) c));
        }
        return buffer.toString();
    }
}