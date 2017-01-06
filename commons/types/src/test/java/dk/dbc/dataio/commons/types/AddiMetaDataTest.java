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

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class AddiMetaDataTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void emptyAddiMetaDataCanBeMarshalledWithoutNullFields() throws JSONBException {
        assertThat(jsonbContext.marshall(new AddiMetaData()), is("{\"deleted\":false}"));
    }

    @Test
    public void emptyAddiMetaDataReturnsNull() {
        final AddiMetaData addiMetaData = new AddiMetaData();
        assertThat("submitterNumber()", addiMetaData.submitterNumber(), is(nullValue()));
        assertThat("format()", addiMetaData.format(), is(nullValue()));
        assertThat("bibliographicRecordId()", addiMetaData.bibliographicRecordId(), is(nullValue()));
        assertThat("trackingId()", addiMetaData.trackingId(), is(nullValue()));
    }

    @Test
    public void accessors() {
        final AddiMetaData addiMetaData = new AddiMetaData()
                .withSubmitterNumber(42)
                .withFormat("marc2")
                .withBibliographicRecordId("id")
                .withTrackingId("trackedAs");
        assertThat("submitterNumber()", addiMetaData.submitterNumber(), is(42));
        assertThat("format()", addiMetaData.format(), is("marc2"));
        assertThat("bibliographicRecordId()", addiMetaData.bibliographicRecordId(), is("id"));
        assertThat("trackingId()", addiMetaData.trackingId(), is("trackedAs"));
    }

    @Test
    public void canBeMarshalledUnmarshalled() throws JSONBException {
        final AddiMetaData addiMetaData = new AddiMetaData()
                .withCreationDate(new Date())
                .withSubmitterNumber(42)
                .withFormat("marc2")
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()
                                        .withAgencyType("theWorstType")
                                        .withLibraryRule("canDeleteAll", true)
                                        .withLibraryRule("canGetAwayWithEverything", true));

        final AddiMetaData unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(addiMetaData), AddiMetaData.class);
        assertThat(unmarshalled, is(addiMetaData));
        assertThat("formattedCreationDate", addiMetaData.formattedCreationDate(), is(notNullValue()));
    }

    @Test
    public void formattedCreationDate() {
        // Below date 2014-12-31 is an example of a date where year is not the same as week-year, ie. yyyy != YYYY
        final Date date = new Date((long)1419984000 * 1000);
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        final AddiMetaData addiMetaData = new AddiMetaData()
                .withCreationDate(date);

        assertThat(addiMetaData.formattedCreationDate(), is(sdf.format(date)));
    }
}