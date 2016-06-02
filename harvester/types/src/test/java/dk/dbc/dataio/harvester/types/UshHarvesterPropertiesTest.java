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
    public void withUri_uriArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withUri(null);
        assertThat(ushHarvesterProperties.getUri(), is(nullValue()));
    }

    @Test
    public void withUri_uriArgCanBeEmpty() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withUri("");
        assertThat(ushHarvesterProperties.getUri(), is(""));
    }

    @Test
    public void withJobClass_jobClassArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withJobClass(null);
        assertThat(ushHarvesterProperties.getJobClass(), is(nullValue()));
    }

    @Test
    public void withJobClass_jobClassArgCanBeEmpty() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withJobClass("");
        assertThat(ushHarvesterProperties.getJobClass(), is(""));
    }

    @Test
    public void withCurrentStatus_currentStatusArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withCurrentStatus(null);
        assertThat(ushHarvesterProperties.getCurrentStatus(), is(nullValue()));
    }

    @Test
    public void withCurrentStatus_currentStatusArgCanBeEmpty() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withCurrentStatus("");
        assertThat(ushHarvesterProperties.getCurrentStatus(), is(""));
    }

    @Test
    public void withLastHarvestFinished_lastHarvestFinishedArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withLastHarvestFinishedDate(null);
        assertThat(ushHarvesterProperties.getLastHarvestFinished(), is(nullValue()));
    }

    @Test
    public void withLastHarvestStarted_lastHarvestStartedArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withLastHarvestStartedDate(null);
        assertThat(ushHarvesterProperties.getLastHarvestStarted(), is(nullValue()));
    }

    @Test
    public void withLastUpdated_lastUpdatedArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withLastUpdatedDate(null);
        assertThat(ushHarvesterProperties.getLastUpdated(), is(nullValue()));
    }

    @Test
    public void withMessage_messageArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withMessage(null);
        assertThat(ushHarvesterProperties.getMessage(), is(nullValue()));
    }

    @Test
    public void withMessage_messageArgCanBeEmpty() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withMessage("");
        assertThat(ushHarvesterProperties.getMessage(), is(""));
    }

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
    public void withNextHarvestSchedule_nextHarvestScheduleArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withNextHarvestSchedule(null);
        assertThat(ushHarvesterProperties.getNextHarvestSchedule(), is(nullValue()));
    }

    @Test
    public void withWithStorageUrl_storageUrlArgCanBeNull() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withStorageUrl(null);
        assertThat(ushHarvesterProperties.getStorageUrl(), is(nullValue()));
    }

    @Test
    public void withWithStorageUrl_storageUrlArgCanBeEmpty() {
        final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties()
                .withStorageUrl("");
        assertThat(ushHarvesterProperties.getStorageUrl(), is(""));
    }


    @Test
    public void verify_jsonMarshallingForUshOaiHarvesterProperties() throws Exception {
        JSONBContext jsonbContext = new JSONBContext();
        final String json = jsonbContext.marshall(newUshHarvesterPropertiesInstance());
        System.out.println(json);
        jsonbContext.unmarshall(json, UshHarvesterProperties.class);
    }

    private UshHarvesterProperties newUshHarvesterPropertiesInstance() {
        return new UshHarvesterProperties()
                .withId(123456)
                .withName("UshHarvesterName")
                .withAmountHarvested(2)
                .withLastHarvestStartedDate(new Date())
                .withCurrentStatus("OK");
    }
}
