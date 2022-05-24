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

package dk.dbc.dataio.addi;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MetaDataTest {
    @Test
    public void getInfoJson_nodeIsMissing_returnsEmptyJson() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                "",
                AddiContextTest.SINK_DIRECTIVES,
                AddiContextTest.UPDATE_SINK_DIRECTIVES);
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getInfoJson(), is("{}"));
    }

    @Test
    public void getInfoJson_nodeExists_returnsSinkDirectives() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                AddiContextTest.ES_DIRECTIVES,
                AddiContextTest.SINK_DIRECTIVES,
                AddiContextTest.UPDATE_SINK_DIRECTIVES);
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getInfoJson(),
                is("{\"submitter\":\"820040\",\"format\":\"katalog\",\"language\":\"dan\",\"contentFrom\":\"820040\"}"));
    }

    @Test
    public void getSinkDirectives_nodeIsMissing_returnsNull() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                AddiContextTest.ES_DIRECTIVES,
                "",
                AddiContextTest.UPDATE_SINK_DIRECTIVES);
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getSinkDirectives(), is(nullValue()));
    }

    @Test
    public void getSinkDirectives_nodeExists_returnsSinkDirectives() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                AddiContextTest.ES_DIRECTIVES,
                AddiContextTest.SINK_DIRECTIVES,
                AddiContextTest.UPDATE_SINK_DIRECTIVES);
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getSinkDirectives(), is(notNullValue()));
    }

    @Test
    public void getUpdateSinkDirectives_nodeIsMissing_returnsNull() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                AddiContextTest.ES_DIRECTIVES,
                AddiContextTest.SINK_DIRECTIVES,
                "");
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getUpdateSinkDirectives(), is(nullValue()));
    }

    @Test
    public void getUpdateSinkDirectives_nodeExists_returnsSinkDirectives() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                AddiContextTest.ES_DIRECTIVES,
                AddiContextTest.SINK_DIRECTIVES,
                AddiContextTest.UPDATE_SINK_DIRECTIVES);
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getUpdateSinkDirectives(), is(notNullValue()));
    }
}
