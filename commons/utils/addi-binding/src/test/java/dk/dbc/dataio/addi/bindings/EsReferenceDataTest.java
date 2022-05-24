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

package dk.dbc.dataio.addi.bindings;

import org.junit.Test;

import static dk.dbc.dataio.addi.AddiContextTest.ES_DIRECTIVES;
import static dk.dbc.dataio.addi.AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EsReferenceDataTest {
    @Test
    public void toXmlString() {
        final EsReferenceData esReferenceData = new EsReferenceData()
                .withEsDirectives(new EsDirectives()
                        .withSubmitter("820040")
                        .withFormat("katalog")
                        .withLanguage("dan")
                        .withContentFrom("820040"));

        assertThat(esReferenceData.toXmlString(), is(String.format(ES_REFERENCE_DATA_XML_TEMPLATE, ES_DIRECTIVES, "", "")));
    }
}
