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
