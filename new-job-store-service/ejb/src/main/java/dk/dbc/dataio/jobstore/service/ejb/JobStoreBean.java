package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Stateless
public class JobStoreBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreBean.class);

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    @EJB
    JSONBBean jsonbBean;

    @EJB
    PgJobStore jobStore;

    public void testAddJob() throws JobStoreException {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<records>"
                + "<record>first</record>"
                + "<record>second</record>"
                + "<record>third</record>"
                + "<record>fourth</record>"
                + "<record>fifth</record>"
                + "<record>sixth</record>"
                + "<record>seventh</record>"
                + "<record>eighth</record>"
                + "<record>ninth</record>"
                + "<record>tenth</record>"
                + "<record>eleventh</record>"
                + "</records>";

        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();
        final JobInputStream jobInputStream = new JobInputStream(new JobSpecificationBuilder().build(), true, 0);
        final DataPartitionerFactory.DataPartitioner dataPartitioner =
                new DefaultXmlDataPartitionerFactory().createDataPartitioner(
                        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8.name());
        jobStore.addJob(jobInputStream, dataPartitioner, null, flow, sink);
    }

    public JobInfoSnapshot addAndScheduleJob(JobInputStream jobInputStream) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        LOGGER.trace("JobSpec: {}", jobInputStream.getJobSpecification());
        FlowBinder flowBinder = getFlowBinderOrThrow(jobInputStream.getJobSpecification());
        Flow flow = getFlowOrThrow(flowBinder.getId());
        Sink sink = getSinkOrThrow(flowBinder.getId());

        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot();
        jobInfoSnapshot.setSinkName(sink.getContent().getName());
        jobInfoSnapshot.setFlowName(flow.getContent().getName());
        jobInfoSnapshot.setSpecification(jobInputStream.getJobSpecification());
        LOGGER.debug("THIS SHOULD NOT BE LOGGED - CURRENTLY LOGS TO AVOID PMD-WARNINGS!: {} {}", flow.getId(), sink.getId());

        LOGGER.debug("addAndScheduleJob for job [{}] took (ms): {}", "", stopWatch.getElapsedTime());

        return jobInfoSnapshot;
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
