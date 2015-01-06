package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobStoreBeanTest {
    private JobStoreBean jobStoreBean;
    private FlowStoreServiceConnectorBean mockedFlowStoreServiceConnectorBean;
    private FlowStoreServiceConnectorException flowStoreServiceConnectorException;

    @Before
    public void setup() {
        mockedFlowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
        flowStoreServiceConnectorException = new FlowStoreServiceConnectorException("msg");
        initializeBean();
    }

    @Test(expected = NullPointerException.class)
    public void addAndScheduleJob_nullArgument_throws() throws JobStoreException {
        jobStoreBean.addAndScheduleJob(null);
        fail("No NullPointerException Thrown");
    }

    @Test
    public void addAndScheduleJob_flowBinderNotFound_throws() throws FlowStoreServiceConnectorException {
        JobInputStream jobInputStream = getJobInputStream();

            when(mockedFlowStoreServiceConnectorBean.getFlowBinder(
                    jobInputStream.getJobSpecification().getPackaging(),
                    jobInputStream.getJobSpecification().getFormat(),
                    jobInputStream.getJobSpecification().getCharset(),
                    jobInputStream.getJobSpecification().getSubmitterId(),
                    jobInputStream.getJobSpecification().getDestination())).thenThrow(flowStoreServiceConnectorException);
        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch (Exception e) {
            assertTrue(e instanceof JobStoreException);
            assertTrue(e.getMessage().contains("Could not retrieve FlowBinder"));
        }
    }

    @Test
    public void addAndScheduleJob_flowNotFound_throws() throws FlowStoreServiceConnectorException{
        JobInputStream jobInputStream = getJobInputStream();
        FlowBinder flowBinder = new FlowBinderBuilder().build();

            when(mockedFlowStoreServiceConnectorBean.getFlowBinder(
                    jobInputStream.getJobSpecification().getPackaging(),
                    jobInputStream.getJobSpecification().getFormat(),
                    jobInputStream.getJobSpecification().getCharset(),
                    jobInputStream.getJobSpecification().getSubmitterId(),
                    jobInputStream.getJobSpecification().getDestination())).thenReturn(flowBinder);

            when(mockedFlowStoreServiceConnectorBean.getFlow(flowBinder.getId())).thenThrow(flowStoreServiceConnectorException);
        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch(Exception e) {
            assertTrue(e instanceof JobStoreException);
            assertTrue(e.getMessage().contains("Could not retrieve Flow"));
        }
    }

    @Test
    public void addAndScheduleJob_sinkNotFound_throws() throws FlowStoreServiceConnectorException{
        JobInputStream jobInputStream = getJobInputStream();
        FlowBinder flowBinder = new FlowBinderBuilder().build();

            when(mockedFlowStoreServiceConnectorBean.getFlowBinder(
                    jobInputStream.getJobSpecification().getPackaging(),
                    jobInputStream.getJobSpecification().getFormat(),
                    jobInputStream.getJobSpecification().getCharset(),
                    jobInputStream.getJobSpecification().getSubmitterId(),
                    jobInputStream.getJobSpecification().getDestination())).thenReturn(flowBinder);

            when(mockedFlowStoreServiceConnectorBean.getSink(flowBinder.getId())).thenThrow(flowStoreServiceConnectorException);
        try {
            jobStoreBean.addAndScheduleJob(jobInputStream);
            fail("No exception thrown by addAndScheduleJob()");
        } catch(Exception e) {
            assertTrue(e instanceof JobStoreException);
            assertTrue(e.getMessage().contains("Could not retrieve Sink"));
        }
    }

    @Test
    public void addAndScheduleJob_validInput_jobIsAdded() throws FlowStoreServiceConnectorException{
        JobInputStream jobInputStream = getJobInputStream();
        FlowBinder flowBinder = new FlowBinderBuilder().build();
        Flow flow = new FlowBuilder().build();
        Sink sink = new SinkBuilder().build();

            when(mockedFlowStoreServiceConnectorBean.getFlowBinder(
                    jobInputStream.getJobSpecification().getPackaging(),
                    jobInputStream.getJobSpecification().getFormat(),
                    jobInputStream.getJobSpecification().getCharset(),
                    jobInputStream.getJobSpecification().getSubmitterId(),
                    jobInputStream.getJobSpecification().getDestination())).thenReturn(flowBinder);

            when(mockedFlowStoreServiceConnectorBean.getFlow(flowBinder.getId())).thenReturn(flow);
            when(mockedFlowStoreServiceConnectorBean.getSink(flowBinder.getId())).thenReturn(sink);
        try {
            JobInfoSnapshot jobInfoSnapshot = jobStoreBean.addAndScheduleJob(jobInputStream);
            assertThat(jobInfoSnapshot, not(nullValue()));
            assertThat(jobInfoSnapshot.getFlowName(), is(flow.getContent().getName()));
            assertThat(jobInfoSnapshot.getSinkName(), is(sink.getContent().getName()));
            assertJobSpecification(jobInfoSnapshot.getSpecification(), jobInputStream.getJobSpecification());
        } catch (Exception e) {
            fail("Unexpected exception thrown by AddAndScheduleJob()");
        }
    }

    /*
     * Private methods
     */

    private void initializeBean(){
        jobStoreBean = new JobStoreBean();
        jobStoreBean.jsonbBean = new JSONBBean();
        jobStoreBean.jsonbBean.initialiseContext();
        jobStoreBean.flowStoreServiceConnectorBean = mockedFlowStoreServiceConnectorBean;
    }

    private JobInputStream getJobInputStream() {
        JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        return new JobInputStream(jobSpecification, false, 3);
    }

    private void assertJobSpecification(JobSpecification jobSpecification1, JobSpecification jobSpecification2) {
        assertThat(jobSpecification1.getSubmitterId(), is(jobSpecification2.getSubmitterId()));
        assertThat(jobSpecification1.getDestination(), is(jobSpecification2.getDestination()));
        assertThat(jobSpecification1.getFormat(), is(jobSpecification2.getFormat()));
        assertThat(jobSpecification1.getResultmailInitials(), is(jobSpecification2.getResultmailInitials()));
        assertThat(jobSpecification1.getPackaging(), is(jobSpecification2.getPackaging()));
        assertThat(jobSpecification1.getMailForNotificationAboutVerification(), is(jobSpecification2.getMailForNotificationAboutVerification()));
        assertThat(jobSpecification1.getMailForNotificationAboutProcessing(), is(jobSpecification2.getMailForNotificationAboutProcessing()));
        assertThat(jobSpecification1.getCharset(), is(jobSpecification2.getCharset()));
        assertThat(jobSpecification1.getDataFile(), is(jobSpecification2.getDataFile()));
    }
}
