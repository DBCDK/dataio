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
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.Before;
import org.junit.Test;

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
            jobStoreBean.addAndScheduleJob(jobInputStream);
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
}
