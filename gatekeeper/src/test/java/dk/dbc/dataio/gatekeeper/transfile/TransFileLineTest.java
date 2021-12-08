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

package dk.dbc.dataio.gatekeeper.transfile;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class TransFileLineTest {
    @Test(expected = NullPointerException.class)
    public void constructor_lineArgIsNull_throws() {
        new TransFile.Line((String) null);
    }

    @Test(expected = NullPointerException.class)
    public void copyConstructor_lineArgIsNull_throws() {
        new TransFile.Line((TransFile.Line) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_lineArgIsEmpty_throws() {
        new TransFile.Line("   ");
    }

    @Test
    public void constructor_lineContainsNoKeyValuePairs_noFieldsExtracted() {
        final String lineValue = "notKeyValuePair";
        final TransFile.Line line = new TransFile.Line(lineValue);
        assertThat("Line value", line.getLine(), is(lineValue));
        assertThat("Number of fields found", line.getFieldNames().size(), is(0));
    }

    @Test
    public void constructor_lineContainsKeyValuePairs_fieldsExtracted() {
        final String lineValue = "b=base1,f=file1,c=latin-1,b=base2";
        final TransFile.Line line = new TransFile.Line(lineValue);
        assertThat("Line value", line.getLine(), is(lineValue));
        assertThat("Fields found", line.getFieldNames(), containsInAnyOrder("b", "f", "c"));
    }

    @Test
    public void getField_whenFieldExists_returnsValue() {
        final String lineValue = "b=base1, f=file1,\tc = latin-1";
        final TransFile.Line line = new TransFile.Line(lineValue);
        assertThat("Field b", line.getField("b"), is("base1"));
        assertThat("Field f", line.getField("f"), is("file1"));
        assertThat("Field c", line.getField("c"), is("latin-1"));
    }

    @Test
    public void getField_whenFieldNotExists_returnsNull() {
        final TransFile.Line line = new TransFile.Line("b=base");
        assertThat("Non existing field", line.getField("f"), is(nullValue()));
    }

    @Test
    public void getLine_whenLineIsModified_returnsModifiedLine() {
        final String content = "f=file1,b=base1,m=mail@company.com";
        final TransFile.Line line = new TransFile.Line(content);
        line.setField("b", "foo");
        line.setField("m", "");
        assertThat(line.getLine(), is("b=foo,f=file1,m="));
    }
}
