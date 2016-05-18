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


public class UshHarvesterPropertiesTest {

    @Test
    public void withWithName_nameArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withName(null);
        assertThat(ushHarvesterProperties.getName(), is(nullValue()));
    }

    @Test
    public void withWithName_nameArgCanBeEmpty() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withName("");
        assertThat(ushHarvesterProperties.getName(), is(""));
    }

    @Test
    public void withWithScheduleString_scheduleStringArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withScheduleString(null);
        assertThat(ushHarvesterProperties.getName(), is(nullValue()));
    }

    @Test
    public void withWithScheduleString_scheduleStringArgCanBeEmpty() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withScheduleString("");
        assertThat(ushHarvesterProperties.getScheduleString(), is(""));
    }

    @Test
    public void withWithLastUpdatedDate_lastUpdatedArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withLastUpdatedDate(null);
        assertThat(ushHarvesterProperties.getLastUpdated(), is(nullValue()));
    }

    @Test
    public void withWithLastHarvestedDate_lastHarvestedDateArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withLastHarvestedDate(null);
        assertThat(ushHarvesterProperties.getLastHarvested(), is(nullValue()));
    }

    @Test
    public void withWithReportedStatus_reportedStatusArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withReportedStatus(null);
        assertThat(ushHarvesterProperties.getReportedStatus(), is(nullValue()));
    }

    @Test
    public void withWithReportedStatus_reportedStatusArgCanBeEmpty() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withReportedStatus("");
        assertThat(ushHarvesterProperties.getReportedStatus(), is(""));
    }

    @Test
    public void withLatestStatus_latestStatusArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withLatestStatus(null);
        assertThat(ushHarvesterProperties.getLatestStatus(), is(nullValue()));
    }

    @Test
    public void withLatestStatus_latestStatusArgCanBeEmpty() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withLatestStatus("");
        assertThat(ushHarvesterProperties.getLatestStatus(), is(""));
    }

    @Test
    public void verify_jsonMarshallingForUshOaiHarvesterProperties() throws Exception {
        JSONBContext jsonbContext = new JSONBContext();
        final String json = jsonbContext.marshall(newUshHarvesterPropertiesInstance());
        jsonbContext.unmarshall(json, UshHarvesterProperties.class);
    }

    private UshHarvesterProperties newUshHarvesterPropertiesInstance() {
        return new UshHarvesterProperties()
                .withJobId(123456)
                .withName("UshHarvesterName")
                .withLastHarvestedDate(new Date())
                .withLatestStatus("OK");
    }
}
