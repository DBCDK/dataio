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

package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.oss.ns.updatemarcxchange.MarcXchangeRecord;
import org.junit.Test;

import javax.xml.bind.JAXBException;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MarcXchangeRecordUnmarshallerTest {

    private final MarcXchangeRecordUnmarshaller marcXchangeRecordUnmarshaller = new MarcXchangeRecordUnmarshaller();

    private final String collection =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                    "<marcx:record format=\"danMARC2\"><marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                    "<marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
                    "</marcx:record>" +
                    "</marcx:collection>";

    @Test
    public void toMarcXchangeRecord_validCollection_returns() throws JAXBException {
            MarcXchangeRecord marcXchangeRecord = marcXchangeRecordUnmarshaller.toMarcXchangeRecord(
                    new ChunkItemBuilder().setData(collection).setId(3).build());

            assertThat(marcXchangeRecord.getMarcXchangeRecordId(), is("3"));
    }

    @Test
    public void toMarcXchangeRecord_invalidCollection_returns() {
        assertThat(() -> marcXchangeRecordUnmarshaller.toMarcXchangeRecord(
                new ChunkItemBuilder().setData("invalid").build()), isThrowing(JAXBException.class));
    }
}
