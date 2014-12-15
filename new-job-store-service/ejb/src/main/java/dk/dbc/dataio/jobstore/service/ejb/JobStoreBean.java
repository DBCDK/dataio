package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class JobStoreBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreBean.class);

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    @EJB
    JSONBBean jsonbBean;

    @EJB
    PgJobStore jobStore;

    public void addAndScheduleJob(JobInputStream jobInputStream) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        LOGGER.trace("JobSpec: {}", jobInputStream.getJobSpecification());
        FlowBinder flowBinder = getFlowBinderOrThrow(jobInputStream.getJobSpecification());
        Flow flow = getFlowOrThrow(flowBinder.getId());
        Sink sink = getSinkOrThrow(flowBinder.getId());

        /*int flowId = jobStore.addEntity(flow);
        int sinkId = jobStore.addEntity(sink);*/

        LOGGER.debug("THIS SHOULD NOT BE LOGGED - CURRENTLY LOGS TO AVOID PMD-WARNINGS!: {} {}", flow.getId(), sink.getId());

        LOGGER.debug("addAndScheduleJob for job [{}] took (ms): {}", "", stopWatch.getElapsedTime());
    }

    // Method is package-private for unittesting purposes
    JobSpecification unmarshallJobSpecDataOrThrow(String jobSpecificationData) throws JobStoreException {
        try {
            JSONBContext context = jsonbBean.getContext();
            return context.unmarshall(jobSpecificationData, JobSpecification.class);
        } catch(JSONBException ex) {
            LOGGER.warn("Could note create a JobSpecification from data: {}", jobSpecificationData);
            throw new JobStoreException("Could not create JobSpecefication from data", ex);
        }
    }

    // Method is package-private for unittesting purposes
    FlowBinder getFlowBinderOrThrow(JobSpecification jobSpec) throws JobStoreException {
        try {
            return flowStoreServiceConnectorBean.getFlowBinder(jobSpec.getPackaging(), jobSpec.getFormat(), jobSpec.getCharset(), jobSpec.getSubmitterId(), jobSpec.getDestination());
        } catch(FlowStoreServiceConnectorException ex) {
            LOGGER.warn("Could not retrieve FlowBinder for jobSpec: {}", jobSpec);
            throw new JobStoreException("Could not retrieve FlowBinder", ex);
        }
    }

    // Method is package-private for unittesting purposes
    Flow getFlowOrThrow(long id) throws JobStoreException {
        try {
            return flowStoreServiceConnectorBean.getFlow(id);
        } catch(FlowStoreServiceConnectorException ex) {
            LOGGER.warn("Could not retrieve Flow for FlowBinder with id: {}", id);
            throw new JobStoreException("Could not retrieve Flow", ex);
        }
    }

    // Method is package-private for unittesting purposes
    Sink getSinkOrThrow(long id) throws JobStoreException {
        try {
            return flowStoreServiceConnectorBean.getSink(id);
        } catch(FlowStoreServiceConnectorException ex) {
            LOGGER.warn("Could not retrieve Sink for FlowBinder with id: {}", id);
            throw new JobStoreException("Could not retrieve Sink", ex);
        }
    }
}
