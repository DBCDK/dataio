package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
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
import java.util.List;

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

        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();
        final Submitter submitter = new SubmitterBuilder().build();
        final JobInputStream jobInputStream = new JobInputStream(new JobSpecificationBuilder().build(), true, 0);
        final DataPartitionerFactory.DataPartitioner dataPartitioner =
                new DefaultXmlDataPartitionerFactory().createDataPartitioner(
                        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8.name());
        final SequenceAnalyserSinkKeyGenerator keyGenerator = new SequenceAnalyserSinkKeyGenerator(sink);
        final FlowStoreReferences flowStoreReferences = createFlowStoreReferences(flowBinder, flow, sink, submitter);
        jobStore.addJob(jobInputStream, dataPartitioner, keyGenerator, flow, sink, flowStoreReferences);
    }

    /**
     * Adds new job in the underlying data store from given job input stream.
     *
     * @param jobInputStream, containing information needed to create job, chunk and item entities
     * @return information snapshot of added job
     * @throws JobStoreException on failure to add job
     */
    public JobInfoSnapshot addAndScheduleJob(JobInputStream jobInputStream) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();

        try {
            final FlowBinder flowBinder = getFlowBinderOrThrow(jobInputStream.getJobSpecification());
            final Flow flow = getFlowOrThrow(flowBinder.getContent().getFlowId());
            final Sink sink = getSinkOrThrow(flowBinder.getContent().getSinkId());
            final Submitter submitter = getSubmitterOrThrow(jobInputStream.getJobSpecification().getSubmitterId());

            FlowStoreReferences flowStoreReferences = createFlowStoreReferences(flowBinder, flow, sink, submitter);
            SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator = getSequenceAnalyserKeyGenerator(flowBinder, sink);
            InputStream inputStream = getInputStream(jobInputStream.getJobSpecification());

            final DataPartitionerFactory.DataPartitioner dataPartitioner =
                    new DefaultXmlDataPartitionerFactory().createDataPartitioner(
                            inputStream, jobInputStream.getJobSpecification().getCharset());

            return jobStore.addJob(jobInputStream, dataPartitioner, sequenceAnalyserKeyGenerator, flow, sink, flowStoreReferences);
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Updates existing job in the underlying data by adding external chunk changes
     * @param chunk the chunk id
     * @return information snap shot of updated job
     * @throws JobStoreException on failure to add chunk
     */
    public JobInfoSnapshot addChunk(ExternalChunk chunk) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            return jobStore.addChunk(chunk);
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Bundles resources referenced by a job into a single object
     * @param jobId the job id
     * @return resources referenced by job
     * @throws JobStoreException on failure to retrieve job
     */
    public ResourceBundle getResourceBundle(int jobId) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            return jobStore.getResourceBundle(jobId);
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Returns job listing based on given criteria
     * @param criteria job listing criteria
     * @return list of information snapshots of selected jobs
     */
    public List<JobInfoSnapshot> listJobs(JobListCriteria criteria) {
        final StopWatch stopWatch = new StopWatch();
        try {
            return jobStore.listJobs(criteria);
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
            throw new JobStoreException("Error in flow-store service communication", e);
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

    // Method is package-private for unit testing purposes
    Submitter getSubmitterOrThrow(long submitterNumber) throws JobStoreException {
        try {
            return flowStoreServiceConnectorBean.getSubmitterBySubmitterNumber(submitterNumber);
        } catch(FlowStoreServiceConnectorException ex) {
            LOGGER.warn("Could not retrieve Submitter for jobInputStream with submitter number: {}", submitterNumber);
            throw new JobStoreException("Could not retrieve Submitter", ex);
        }
    }

    /**
     * Method retrieving job data file
     *
     * @param jobSpecification job specification
     * @return input stream
     * @throws JobStoreException on failure on retrieving job data file
     */
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

    /**
     * Method building flow store references based on the flow store entities given as input
     *
     * @param flowBinder to map
     * @param flow to map
     * @param sink to map
     * @param submitter to map
     * @return flow store references
     */
    private FlowStoreReferences createFlowStoreReferences(FlowBinder flowBinder, Flow flow, Sink sink, Submitter submitter) {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, createFlowBinderReference(flowBinder));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW, createFlowReference(flow));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK, createSinkReference(sink));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER, createSubmitterReference(submitter));
        return flowStoreReferences;
    }

    private FlowStoreReference createFlowBinderReference(FlowBinder flowBinder) {
        return new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(),flowBinder.getContent().getName());
    }

    private FlowStoreReference createFlowReference(Flow flow) {
        return new FlowStoreReference(flow.getId(), flow.getVersion(),flow.getContent().getName());
    }

    private FlowStoreReference createSinkReference(Sink sink) {
        return new FlowStoreReference(sink.getId(), sink.getVersion(),sink.getContent().getName());
    }

    private FlowStoreReference createSubmitterReference(Submitter submitter) {
        return new FlowStoreReference(submitter.getId(), submitter.getVersion(),submitter.getContent().getName());
    }

}
