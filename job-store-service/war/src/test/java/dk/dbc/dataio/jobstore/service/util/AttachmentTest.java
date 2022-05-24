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

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AttachmentTest {
    @Test
    public void decipherCharset() {
        assertThat("latin1", Attachment.decipherCharset("latin1"), is(StandardCharsets.ISO_8859_1));
        assertThat("utf8", Attachment.decipherCharset("utf8"), is(StandardCharsets.UTF_8));
    }

    @Test
    public void decipherCharset_charsetNameArgIsUnknown_throws() {
        assertThat(() -> Attachment.decipherCharset("not-a-charset"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void decipherFileNameExtensionFromPackaging() {
        assertThat("ISO", Attachment.decipherFileNameExtensionFromPackaging("ISO"), is("iso2709"));
        assertThat("LIN", Attachment.decipherFileNameExtensionFromPackaging("LIN"), is("lin"));
        assertThat("XML", Attachment.decipherFileNameExtensionFromPackaging("XML"), is("xml"));
        assertThat("ADDI-XML", Attachment.decipherFileNameExtensionFromPackaging("ADDI-XML"), is("xml"));
        assertThat("UNKNOWN", Attachment.decipherFileNameExtensionFromPackaging("UNKNOWN"), is("txt"));
    }
}
