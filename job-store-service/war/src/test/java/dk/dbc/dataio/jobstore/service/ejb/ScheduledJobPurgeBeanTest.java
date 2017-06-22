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

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnectorUnexpectedStatusCodeException;
import org.junit.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class ScheduledJobPurgeBeanTest {
    private JobPurgeBean jobPurgeBean = mock(JobPurgeBean.class);


    @Test
    public void run_scheduledJobPurgeBeanThrowsUncheckedException_noExceptionThrown() throws FileStoreServiceConnectorException, LogStoreServiceConnectorUnexpectedStatusCodeException {
        final ScheduledJobPurgeBean scheduledJobPurgeBean = createScheduledJobPurgeBean();
        doThrow(new RuntimeException("DIED")).when(jobPurgeBean).purgeJobs();
        scheduledJobPurgeBean.run();
    }

    @Test
    public void run_scheduledJobPurgeBeanThrowsCheckedException_noExceptionThrown() throws LogStoreServiceConnectorUnexpectedStatusCodeException, FileStoreServiceConnectorException {
        final ScheduledJobPurgeBean bean = createScheduledJobPurgeBean();
        doThrow(new LogStoreServiceConnectorUnexpectedStatusCodeException("DIED", 404)).when(jobPurgeBean).purgeJobs();
        bean.run();
    }

    private ScheduledJobPurgeBean createScheduledJobPurgeBean() {
        final ScheduledJobPurgeBean scheduledJobPurgeBean = new ScheduledJobPurgeBean();
        scheduledJobPurgeBean.jobPurgeBean = jobPurgeBean;
        return scheduledJobPurgeBean;
    }
}