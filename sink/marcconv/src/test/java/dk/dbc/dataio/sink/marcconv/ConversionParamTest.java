/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.sink.marcconv;

import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Marc8Charset;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ConversionParamTest {
    @Test
    public void getEncoding() {
        final ConversionParam param = new ConversionParam();
        assertThat("null", param.getEncoding().orElse(null),
                is(nullValue()));
        assertThat("danmarc2", param.withEncoding("danmarc2").getEncoding().orElse(null),
                is(new DanMarc2Charset()));
        assertThat("latin1", param.withEncoding("latin1").getEncoding().orElse(null),
                is(StandardCharsets.ISO_8859_1));
        assertThat("marc8", param.withEncoding("marc8").getEncoding().orElse(null),
                is(new Marc8Charset()));
        assertThat("utf8", param.withEncoding("utf8").getEncoding().orElse(null),
                is(StandardCharsets.UTF_8));
    }

    @Test
    public void getEncoding_unknownEncoding() {
        final ConversionParam param = new ConversionParam();
        assertThat(() -> param.withEncoding("unknown").getEncoding(), isThrowing(ConversionException.class));
    }

    @Test
    public void unmarshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final String json = "{\"encoding\": \"utf8\", \"magicNumber\": 42}";
        final ConversionParam param = jsonbContext.unmarshall(json, ConversionParam.class);
        assertThat(param.getEncoding().orElse(null), is(StandardCharsets.UTF_8));
    }
}