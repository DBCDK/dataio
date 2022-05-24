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

package dk.dbc.dataio.harvester.utils.datafileverifier;

import dk.dbc.dataio.commons.utils.lang.StringUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class Expectation {
    public final Charset encoding;
    public final byte[] expectedData;

    public Expectation() {
        this((byte[]) null, null);
    }

    public Expectation(String expectedData) {
        this(expectedData.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    public Expectation(String expectedData, Charset encoding) {
        this(expectedData.getBytes(encoding), encoding);
    }

    public Expectation(byte[] expectedData) {
        this(expectedData, StandardCharsets.UTF_8);
    }

    public Expectation(byte[] expectedData, Charset encoding) {
        this.expectedData = expectedData;
        this.encoding = encoding;
    }

    public void verify(byte[] data) {
        assertThat("data", StringUtil.asString(data, encoding), is(StringUtil.asString(expectedData, encoding)));
    }
}
