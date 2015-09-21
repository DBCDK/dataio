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

package dk.dbc.dataio.jobstore.service.ejb.monitoring;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SequenceAnalyserMonitorSampleTest {
    @Test
    public void constructor_returnsNewInstance() {
        final long queued = 42;
        final long headOfQueueMonitoringStartTime = 424242;
        final SequenceAnalyserMonitorSample sequenceAnalyserMonitorSample =
                new SequenceAnalyserMonitorSample(queued, headOfQueueMonitoringStartTime);
        assertThat(sequenceAnalyserMonitorSample, is(notNullValue()));
        assertThat(sequenceAnalyserMonitorSample.getQueued(), is(queued));
        assertThat(sequenceAnalyserMonitorSample.getHeadOfQueueMonitoringStartTime(), is(headOfQueueMonitoringStartTime));
        assertThat(sequenceAnalyserMonitorSample.getHeadOfQueueWaitTimeInMs(), is(not(0L)));
    }

    @Test
    public void getHeadOfQueueWaitTimeInMs_queuedIsZero_returnsZero() {
        final long queued = 0;
        final long headOfQueueMonitoringStartTime = 424242;
        final SequenceAnalyserMonitorSample sequenceAnalyserMonitorSample =
                new SequenceAnalyserMonitorSample(queued, headOfQueueMonitoringStartTime);
        assertThat(sequenceAnalyserMonitorSample.getHeadOfQueueWaitTimeInMs(), is(0L));
    }
}