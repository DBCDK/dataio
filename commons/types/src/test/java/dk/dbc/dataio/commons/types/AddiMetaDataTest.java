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

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class AddiMetaDataTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void emptyAddiMetaDataCanBeMarshalledWithoutNullFields() throws JSONBException {
        assertThat(jsonbContext.marshall(new AddiMetaData()), is("{}"));
    }

    @Test
    public void emptyAddiMetaDataReturnsNull() {
        final AddiMetaData addiMetaData = new AddiMetaData();
        assertThat("submitterNumber()", addiMetaData.submitterNumber(), is(nullValue()));
        assertThat("format()", addiMetaData.format(), is(nullValue()));
        assertThat("bibliographicRecordId()", addiMetaData.bibliographicRecordId(), is(nullValue()));
        assertThat("trackingId()", addiMetaData.trackingId(), is(nullValue()));
        assertThat("pid()", addiMetaData.pid(), is(nullValue()));
        assertThat("ocn()", addiMetaData.ocn(), is(nullValue()));
        assertThat("holdingsStatusMap()", addiMetaData.holdingsStatusMap(), is(nullValue()));
    }

    @Test
    public void accessors() {
        Map<String, Integer> statusMap = new HashMap<>();
        statusMap.put("OnShelf", 32);
        final AddiMetaData addiMetaData = new AddiMetaData()
                .withSubmitterNumber(42)
                .withFormat("marc2")
                .withBibliographicRecordId("id")
                .withTrackingId("trackedAs")
                .withPid("pid")
                .withOcn("ocn")
                .withHoldingsStatusMap(statusMap);
        assertThat("submitterNumber()", addiMetaData.submitterNumber(), is(42));
        assertThat("format()", addiMetaData.format(), is("marc2"));
        assertThat("bibliographicRecordId()", addiMetaData.bibliographicRecordId(), is("id"));
        assertThat("trackingId()", addiMetaData.trackingId(), is("trackedAs"));
        assertThat("pid()", addiMetaData.pid(), is("pid"));
        assertThat("ocn()", addiMetaData.ocn(), is("ocn"));
        assertThat("holdingsStatusMap()", addiMetaData.holdingsStatusMap(), is(statusMap));
    }

    @Test
    public void canBeMarshalledUnmarshalled() throws JSONBException {
        Map<String, Integer> statusMap = new HashMap<>();
        statusMap.put("OnShelf", 32);
        final AddiMetaData addiMetaData = new AddiMetaData()
                .withCreationDate(new Date())
                .withSubmitterNumber(42)
                .withFormat("marc2")
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()
                                        .withAgencyType("theWorstType")
                                        .withLibraryRule("canDeleteAll", true)
                                        .withLibraryRule("canGetAwayWithEverything", true)
                                        .withLibraryRule("isNotBoolean", "value"))
                .withPid("pid")
                .withOcn("ocn")
                .withHoldingsStatusMap(statusMap);

        final AddiMetaData unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(addiMetaData), AddiMetaData.class);
        assertThat(unmarshalled, is(addiMetaData));
        assertThat("formattedCreationDate", addiMetaData.formattedCreationDate(), is(notNullValue()));
    }

    @Test
    public void formattedCreationDate() {
        // a date where year is not the same as week-year, ie. yyyy != YYYY)
        AddiMetaData addiMetaData = new AddiMetaData()
                .withCreationDate(new Date(1419980400000L));
        assertThat("Wed Dec 31 00:00:00 CET 2014", addiMetaData.formattedCreationDate(), is("20141231"));

        addiMetaData = new AddiMetaData()
                .withCreationDate(new Date(1406757600000L));
        assertThat("Thu Jul 31 00:00:00 CEST 2014", addiMetaData.formattedCreationDate(), is("20140731"));
    }
}
