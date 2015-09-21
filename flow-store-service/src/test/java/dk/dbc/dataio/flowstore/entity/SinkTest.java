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

package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
  * Sink unit tests
  *
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
public class SinkTest {
    @Test
    public void setContent_jsonDataArgIsValidFlowContentJson_setsNameIndexValue() throws Exception {
        final String name = "testSink";
        final String sinkContent = new SinkContentJsonBuilder()
                .setName(name)
                .build();

        final Sink sink = getSinkEntity();
        sink.setContent(sinkContent);
        assertThat(sink.getNameIndexValue(), is(name));
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidSinkContent_throws() throws Exception {
        getSinkEntity().setContent("{}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        getSinkEntity().setContent("{");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        getSinkEntity().setContent("");
    }

    @Test(expected = NullPointerException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        getSinkEntity().setContent(null);
    }

    public static Sink getSinkEntity() {
        return new Sink();
    }
}
