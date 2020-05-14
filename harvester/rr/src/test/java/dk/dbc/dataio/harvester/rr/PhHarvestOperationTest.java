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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.phlog.PhLog;
import dk.dbc.phlog.dto.PhLogEntry;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PhHarvestOperationTest extends HarvestOperationTest {
    private final PhLog phLog = new PhLog(entityManager);

    public static final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Meter meter = mock(Meter.class);
    private final Timer timer = mock(Timer.class);

    @Before
    public void setupMockedPhLog() {
        final PhLogEntry phLogEntry = new PhLogEntry();
        when(entityManager.find(eq(PhLogEntry.class), any(PhLogEntry.Key.class))).thenReturn(phLogEntry);
        when(metricRegistry.meter(any(Metadata.class), any(Tag.class))).thenReturn(meter);
        when(metricRegistry.timer(any(Metadata.class), any(Tag.class))).thenReturn(timer);
        doNothing().when(meter).mark();
        doNothing().when(timer).update(anyLong(), any());
    }

    @Test
    public void execute_phLogHasRecordMarkedAsDelete_recordIsMarkedAsDelete()
            throws HarvesterException, JSONBException {
        final HarvestOperation harvestOperation = newHarvestOperation();
        final PhLogEntry phLogEntry = new PhLogEntry().withDeleted(true);
        when(entityManager.find(eq(PhLogEntry.class), any(PhLogEntry.Key.class))).thenReturn(phLogEntry);
        harvestOperation.execute();
        final ArgumentCaptor<AddiRecord> addiRecordCaptor = ArgumentCaptor.forClass(AddiRecord.class);
        verify(harvesterJobBuilder, times(1)).addRecord(addiRecordCaptor.capture());
        final AddiRecord addiRecord = addiRecordCaptor.getValue();
        final JSONBContext jsonbContext = new JSONBContext();
        final AddiMetaData addiMetaData = jsonbContext.unmarshall(
                new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);
        assertThat(addiMetaData.isDeleted(), is(true));
    }

    @Test
    public void execute_recordHasNoEntryInPhLog_recordIsSkipped() throws HarvesterException {
        final HarvestOperation harvestOperation = newHarvestOperation();
        when(entityManager.find(eq(PhLogEntry.class), any(PhLogEntry.Key.class))).thenReturn(null);
        harvestOperation.execute();
        verify(harvesterJobBuilder, times(0)).addRecord(any(AddiRecord.class));
    }

    @Override
    public void execute_rawRepoDeleteRecordHasAgencyIdContainedInExcludedSet_recordIsProcessed() {
        // Irrelevant test from super class
    }

    @Override
    public void execute_rawRepoDeleteRecordHasDbcId_recordIsSkipped() {
        // Irrelevant test from super class
    }

    @Override
    public HarvestOperation newHarvestOperation() {
        return newHarvestOperation(HarvesterTestUtil.getRRHarvesterConfig());
    }

    @Override
    public HarvestOperation newHarvestOperation(RRHarvesterConfig config) {
        try {
            return new PhHarvestOperation(config, harvesterJobBuilderFactory, taskRepo,
                    new AgencyConnection(OPENAGENCY_ENDPOINT), rawRepoConnector, phLog,
                    rawRepoRecordServiceConnector, metricRegistry);
        } catch (SQLException | QueueException | ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }
}