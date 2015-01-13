package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserNoOrderKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Stateless
public class JobStoreBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreBean.class);

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    @EJB
    JSONBBean jsonbBean;

    @EJB
    PgJobStore jobStore;

    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

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
        final SequenceAnalyserSinkKeyGenerator keyGenerator = new SequenceAnalyserSinkKeyGenerator(sink);
        jobStore.addJob(jobInputStream, dataPartitioner, keyGenerator, flow, sink);
    }

    public JobInfoSnapshot addAndScheduleJob(JobInputStream jobInputStream) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();

        try {
            final FlowBinder flowBinder = getFlowBinderOrThrow(jobInputStream.getJobSpecification());
            final Flow flow = getFlowOrThrow(flowBinder.getId());
            final Sink sink = getSinkOrThrow(flowBinder.getId());

            SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator = getSequenceAnalyserKeyGenerator(flowBinder, sink);
            InputStream inputStream = getInputStream(jobInputStream.getJobSpecification());

            final DataPartitionerFactory.DataPartitioner dataPartitioner =
                    new DefaultXmlDataPartitionerFactory().createDataPartitioner(
                            inputStream, jobInputStream.getJobSpecification().getCharset());

            return jobStore.addJob(jobInputStream, dataPartitioner, sequenceAnalyserKeyGenerator, flow, sink);

        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    // Method is package-private for unit testing purposes
    FlowBinder getFlowBinderOrThrow(JobSpecification jobSpec) throws JobStoreException {
        try {
            return flowStoreServiceConnectorBean.getFlowBinder(
                    jobSpec.getPackaging(),
                    jobSpec.getFormat(),
                    jobSpec.getCharset(),
                    jobSpec.getSubmitterId(),
                    jobSpec.getDestination());

        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            LOGGER.warn("Could not retrieve FlowBinder for jobSpec: {}", jobSpec);
            // If status is NOT_FOUND (404)
            if (e.getStatusCode() == 404) {
                final JobError jobError = new JobError(JobError.Code.INVALID_FLOW_BINDER_IDENTIFIER, e.getMessage(), ServiceUtil.stackTraceToString(e));
                throw new InvalidInputException("flow binder could not be found.", jobError);
            } else {
                throw new JobStoreException("Error retrieving FlowBinder", e);
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new JobStoreException("flow-store service returned with null-valued FlowBinder", e);
        }
    }

    // Method is package-private for unit testing purposes
    Flow getFlowOrThrow(long id) throws JobStoreException {
        try {
            return flowStoreServiceConnectorBean.getFlow(id);
        } catch(FlowStoreServiceConnectorException ex) {
            LOGGER.warn("Could not retrieve Flow for FlowBinder with id: {}", id);
            throw new JobStoreException("Could not retrieve Flow", ex);
        }
    }

    // Method is package-private for unit testing purposes
    Sink getSinkOrThrow(long id) throws JobStoreException {
        try {
            return flowStoreServiceConnectorBean.getSink(id);
        } catch(FlowStoreServiceConnectorException ex) {
            LOGGER.warn("Could not retrieve Sink for FlowBinder with id: {}", id);
            throw new JobStoreException("Could not retrieve Sink", ex);
        }
    }

    // Method is package-private for unit testing purposes
    SequenceAnalyserKeyGenerator getSequenceAnalyserKeyGenerator(FlowBinder flowBinder, Sink sink) {
        if(flowBinder.getContent().getSequenceAnalysis()) {
            return new SequenceAnalyserSinkKeyGenerator(sink);
        } else {
            return new SequenceAnalyserNoOrderKeyGenerator();
        }
    }

    private InputStream getInputStream (JobSpecification jobSpecification) throws JobStoreException {
        String jobDataFile = jobSpecification.getDataFile();
        boolean isLocalFile = Files.exists(Paths.get(jobDataFile));
        try {
            return isLocalFile ? Files.newInputStream(Paths.get(jobDataFile)) : fileStoreServiceConnectorBean.getFile(new FileStoreUrn(jobDataFile).getFileId());
        } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
            // If status is NOT_FOUND (404)
            if(e.getStatusCode() == 404) {
                final JobError jobError = new JobError(JobError.Code.INVALID_DATAFILE, e.getMessage(), ServiceUtil.stackTraceToString(e));
                throw new InvalidInputException("Job data file could not be found", jobError);
            } else {
                throw new JobStoreException("Error retrieving job data file", e);
            }
        } catch (URISyntaxException e) {
            JobError jobError = new JobError(JobError.Code.INVALID_URI_SYNTAX, e.getMessage(), ServiceUtil.stackTraceToString(e));
            throw new InvalidInputException("Invalid file store service URI", jobError);
        } catch (FileStoreServiceConnectorException | IOException e) {
            throw new JobStoreException(e.getMessage(), e);
        }
    }
}
