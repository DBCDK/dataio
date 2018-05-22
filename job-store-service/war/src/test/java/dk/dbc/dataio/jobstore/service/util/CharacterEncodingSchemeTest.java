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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.marc.Marc8Charset;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CharacterEncodingSchemeTest {
    private final Charset marc8 = new Marc8Charset();
    @Test
    public void charsetOf_normalizesName() {
        assertThat("latin1", CharacterEncodingScheme.charsetOf("latin1"), is(StandardCharsets.ISO_8859_1));
        assertThat("LATIN-1", CharacterEncodingScheme.charsetOf("LATIN-1"), is(StandardCharsets.ISO_8859_1));
        assertThat("ISO-8859-1", CharacterEncodingScheme.charsetOf("ISO-8859-1"), is(StandardCharsets.ISO_8859_1));
        assertThat("utf8", CharacterEncodingScheme.charsetOf("UTF-8"), is(StandardCharsets.UTF_8));
        assertThat("UTF-8", CharacterEncodingScheme.charsetOf("UTF-8"), is(StandardCharsets.UTF_8));
        assertThat("marc-8", CharacterEncodingScheme.charsetOf("marc-8"), is(marc8));
        assertThat("MARC8", CharacterEncodingScheme.charsetOf("MARC8"), is(marc8));
    }

    @Test
    public void charsetOf_throwsWhenUnableToResolveName() {
        assertThat(() -> CharacterEncodingScheme.charsetOf("unknown"), isThrowing(InvalidEncodingException.class));
    }
}