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

package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.jsonb.JSONBContext;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;


public class UshOaiHarvesterPropertiesTest {

    @Test
    public void withWithName_nameArgCanBeNull() {
        final UshOaiHarvesterProperties ushOaiHarvesterProperties = new UshOaiHarvesterProperties()
                .withName(null);
        assertThat(ushOaiHarvesterProperties.getName(), is(nullValue()));
    }

    @Test
    public void withWithName_nameArgCanBeEmpty() {
        final UshOaiHarvesterProperties ushOaiHarvesterProperties = new UshOaiHarvesterProperties()
                .withName("");
        assertThat(ushOaiHarvesterProperties.getName(), is(""));
    }

    @Test
    public void withWithScheduleString_scheduleStringArgCanBeNull() {
        final UshOaiHarvesterProperties ushOaiHarvesterProperties = new UshOaiHarvesterProperties()
                .withScheduleString(null);
        assertThat(ushOaiHarvesterProperties.getName(), is(nullValue()));
    }

    @Test
    public void withWithScheduleString_scheduleStringArgCanBeEmpty() {
        final UshOaiHarvesterProperties ushOaiHarvesterProperties = new UshOaiHarvesterProperties()
                .withScheduleString("");
        assertThat(ushOaiHarvesterProperties.getScheduleString(), is(""));
    }

    @Test
    public void withWithLastUpdatedDate_lastUpdatedArgCanBeNull() {
        final UshOaiHarvesterProperties ushOaiHarvesterProperties = new UshOaiHarvesterProperties()
                .withLastUpdatedDate(null);
        assertThat(ushOaiHarvesterProperties.getLastUpdated(), is(nullValue()));
    }

    @Test
    public void withWithLastHarvestedDate_lastHarvestedDateArgCanBeNull() {
        final UshOaiHarvesterProperties ushOaiHarvesterProperties = new UshOaiHarvesterProperties()
                .withLastHarvestedDate(null);
        assertThat(ushOaiHarvesterProperties.getLastHarvested(), is(nullValue()));
    }

    @Test
    public void withWithReportedStatus_reportedStatusArgCanBeNull() {
        final UshOaiHarvesterProperties ushOaiHarvesterProperties = new UshOaiHarvesterProperties()
                .withReportedStatus(null);
        assertThat(ushOaiHarvesterProperties.getReportedStatus(), is(nullValue()));
    }

    @Test
    public void withWithReportedStatus_reportedStatusArgCanBeEmpty() {
        final UshOaiHarvesterProperties ushOaiHarvesterProperties = new UshOaiHarvesterProperties()
                .withReportedStatus("");
        assertThat(ushOaiHarvesterProperties.getReportedStatus(), is(""));
    }

    @Test
    public void withLatestStatus_latestStatusArgCanBeNull() {
        final UshOaiHarvesterProperties ushOaiHarvesterProperties = new UshOaiHarvesterProperties()
                .withLatestStatus(null);
        assertThat(ushOaiHarvesterProperties.getLatestStatus(), is(nullValue()));
    }

    @Test
    public void withLatestStatus_latestStatusArgCanBeEmpty() {
        final UshOaiHarvesterProperties ushOaiHarvesterProperties = new UshOaiHarvesterProperties()
                .withLatestStatus("");
        assertThat(ushOaiHarvesterProperties.getLatestStatus(), is(""));
    }

    @Test
    public void verify_jsonMarshallingForUshOaiHarvesterProperties() throws Exception {
        JSONBContext jsonbContext = new JSONBContext();
        final String json = jsonbContext.marshall(newUshOaiHarvesterPropertiesInstance());
        jsonbContext.unmarshall(json, UshOaiHarvesterProperties.class);
    }

    private UshOaiHarvesterProperties newUshOaiHarvesterPropertiesInstance() {
        return new UshOaiHarvesterProperties()
                .withJobId(123456)
                .withName("UshOaiHarvesterName")
                .withLastHarvestedDate(new Date())
                .withLatestStatus("OK");
    }
}
