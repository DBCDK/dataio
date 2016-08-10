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

package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AddiMetaDataTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void emptyAddiMetaData_canBeMarshalledWithoutNullFields() throws JSONBException {
        assertThat(jsonbContext.marshall(new AddiMetaData()), is("{}"));
    }

    @Test
    public void emptyAddiMetaData_gettersReturnEmptyOptional() {
        final AddiMetaData addiMetaData = new AddiMetaData();
        assertThat("submitterNumber()", addiMetaData.submitterNumber(), is(Optional.empty()));
        assertThat("format()", addiMetaData.format(), is(Optional.empty()));
        assertThat("bibliographicRecordId()", addiMetaData.bibliographicRecordId(), is(Optional.empty()));
        assertThat("trackingId()", addiMetaData.trackingId(), is(Optional.empty()));
    }

    @Test
    public void addiMetaData_setters() {
        final AddiMetaData addiMetaData = new AddiMetaData()
                .withSubmitterNumber(42)
                .withFormat("marc2")
                .withBibliographicRecordId("id")
                .withTrackingId("trackedAs");
        assertThat("submitterNumber()", addiMetaData.submitterNumber().orElse(null), is(42));
        assertThat("format()", addiMetaData.format().orElse(null), is("marc2"));
        assertThat("bibliographicRecordId()", addiMetaData.bibliographicRecordId().orElse(null), is("id"));
        assertThat("trackingId()", addiMetaData.trackingId().orElse(null), is("trackedAs"));
    }

    @Test
    public void addiMetaData_canBeMarshalledUnmarshalled() throws JSONBException {
        final AddiMetaData addiMetaData = new AddiMetaData()
                .withSubmitterNumber(42)
                .withFormat("marc2")
                .withLibraryRules(new AddiMetaData.LibraryRules()
                                        .withAgencyType("theWorstType")
                                        .withLibraryRule("canDeleteAll", true)
                                        .withLibraryRule("canGetAwayWithEverything", true));
        final AddiMetaData unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(addiMetaData), AddiMetaData.class);
        assertThat(unmarshalled, is(addiMetaData));
    }
}