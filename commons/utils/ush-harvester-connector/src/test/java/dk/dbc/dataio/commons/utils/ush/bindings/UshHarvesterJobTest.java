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

package dk.dbc.dataio.commons.utils.ush.bindings;

import dk.dbc.commons.testutil.Assert;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import org.junit.Test;

import java.time.format.DateTimeParseException;

import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UshHarvesterJobTest {

    @Test
    public void toDate_invalidInput_throws() {
        Assert.assertThat(() -> UshHarvesterJob.toDate("invalid"), isThrowing(DateTimeParseException.class));
    }

    @Test
    public void toDate_inputIsNull_returnsNull() {
        assertThat("UshHarvesterJob.toDate()", UshHarvesterJob.toDate(null), is(nullValue()));
    }

    @Test
    public void toUshHarvesterProperties_noValues_ok() {
        assertThat("UshHarvesterProperties", new UshHarvesterJob().toUshHarvesterProperties(), is(new UshHarvesterProperties()));
    }

    @Test
    public void toUshHarvesterProperties_withValues_ok() {
        final UshHarvesterJob ushHarvesterJob = new UshHarvesterJob();
        ushHarvesterJob.setId(1);
        ushHarvesterJob.setName("Name");
        ushHarvesterJob.setScheduleString("0 0 * * *");
        ushHarvesterJob.setLastHarvested("Mon May 09 10:28:33 CEST 2016");
        ushHarvesterJob.setLastUpdated("Tue May 10 11:03:45 CEST 2016");
        ushHarvesterJob.setLatestStatus("OK");
        ushHarvesterJob.setReportedStatus("OK");
        ushHarvesterJob.setError("Error");

        final UshHarvesterProperties expectedUshHarvesterProperties = new UshHarvesterProperties()
                .withJobId(ushHarvesterJob.getId())
                .withName(ushHarvesterJob.getName())
                .withScheduleString(ushHarvesterJob.getScheduleString())
                .withLastHarvestedDate(UshHarvesterJob.toDate(ushHarvesterJob.getLastHarvested()))
                .withLastUpdatedDate(UshHarvesterJob.toDate(ushHarvesterJob.getLastUpdated()))
                .withLatestStatus(ushHarvesterJob.getLatestStatus())
                .withReportedStatus(ushHarvesterJob.getReportedStatus())
                .withError(ushHarvesterJob.getError());
        assertThat("UshHarvesterProperties", ushHarvesterJob.toUshHarvesterProperties(), is(expectedUshHarvesterProperties));
    }

}
