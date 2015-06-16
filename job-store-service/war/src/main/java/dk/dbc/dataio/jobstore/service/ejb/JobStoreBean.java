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
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserNoOrderKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Stateless
public class JobStoreBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreBean.class);

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    @EJB
    PgJobStore jobStore;

    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

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

            String fileId = getFileIdFromDataFile(jobInputStream.getJobSpecification().getDataFile());
            try (InputStream inputStream = getInputStream(jobInputStream.getJobSpecification().getDataFile(), fileId)) {
                final DataPartitionerFactory.DataPartitioner dataPartitioner =
                        new DefaultXmlDataPartitionerFactory().createDataPartitioner(
                                inputStream, jobInputStream.getJobSpecification().getCharset());

                final JobInfoSnapshot jobInfoSnapshot = jobStore.addJob(jobInputStream, dataPartitioner, sequenceAnalyserKeyGenerator, flow, sink, flowStoreReferences);
                if(fileId != null) {
                    // Compare byte size only if the file is located in the file store
                    compareByteSize(fileId, dataPartitioner);
                }
                return jobInfoSnapshot;
            } catch (IOException e) {
                throw new JobStoreException("Error reading data file", e);
            }
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

/*    public JobInfoSnapshot addAndScheduleJob(JobInputStream jobInputStream) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try (AddJobParam param = new AddJobParam(jobInputStream, flowStoreServiceConnectorBean.getConnector(), fileStoreServiceConnectorBean.getConnector()) {
            final JobInfoSnapshot jobInfoSnapshot = jobStore.addJob(param);
            if (!jobInfoSnapshot.getState().fatalDiagnosticExists()) {
                compareByteSize(param.getDataFileId(), param.getDataPartitioner());
            }
            return jobInfoSnapshot;
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }*/


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

    /**
     * Returns item listing based on given criteria
     * @param criteria item listing criteria
     * @return list of information snapshots of selected items
     */
    public List<ItemInfoSnapshot> listItems(ItemListCriteria criteria) {
        final StopWatch stopWatch = new StopWatch();
        try {
            return jobStore.listItems(criteria);
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves chunk item
     * @param jobId the job id
     * @param chunkId the chunk id
     * @param itemId the item id
     * @return chunk item
     * @throws JobStoreException on failure to retrieve itemEntity
     */
    public ItemData getItemData(int jobId, int chunkId, short itemId, State.Phase phase) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            return jobStore.getItemData(jobId, chunkId, itemId, phase);
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    // Method is package-private for unit testing purposes
    FlowBinder getFlowBinderOrThrow(JobSpecification jobSpec) throws JobStoreException {
        try {
            return flowStoreServiceConnectorBean.getConnector().getFlowBinder(
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
            return flowStoreServiceConnectorBean.getConnector().getFlow(id);
        } catch(FlowStoreServiceConnectorException ex) {
            LOGGER.warn("Could not retrieve Flow for FlowBinder with id: {}", id);
            throw new JobStoreException("Could not retrieve Flow", ex);
        }
    }

    // Method is package-private for unit testing purposes
    Sink getSinkOrThrow(long id) throws JobStoreException {
        try {
            return flowStoreServiceConnectorBean.getConnector().getSink(id);
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
            return flowStoreServiceConnectorBean.getConnector().getSubmitterBySubmitterNumber(submitterNumber);
        } catch(FlowStoreServiceConnectorException ex) {
            LOGGER.warn("Could not retrieve Submitter for jobInputStream with submitter number: {}", submitterNumber);
            throw new JobStoreException("Could not retrieve Submitter", ex);
        }
    }

    // Method is package-private for unit testing purposes
    long getByteSizeOrThrow(String fileId) throws JobStoreException {
        try {
            return fileStoreServiceConnectorBean.getConnector().getByteSize(fileId);
        } catch(FileStoreServiceConnectorException ex) {
            LOGGER.warn("Could not retrieve byte size for file with id: {}", fileId);
            throw new JobStoreException("Could not retrieve byte size", ex);
        }
    }

    /**
     * Method is package-private for unit testing purposes
     * Method compares bytes size of a file with the byte size of the data partitioner used when adding a job to the job store
     *
     * @param fileId file id
     * @param dataPartitioner containing the input steam
     * @throws IOException if the byte size differs
     * @throws JobStoreException if the byte size could not be retrieved, InvalidInputException if the file store service URI was invalid
     */
      void compareByteSize(String fileId, DataPartitionerFactory.DataPartitioner dataPartitioner) throws IOException, JobStoreException {
        long fileByteSize = getByteSizeOrThrow(fileId);
        long jobByteSize = dataPartitioner.getBytesRead();

        if(fileByteSize != jobByteSize){
            throw new IOException(String.format(
                    "Error reading data file {%s}. DataPartitioner.byteSize was: %s. FileStore.byteSize was: %s",
                    fileId, jobByteSize, fileByteSize));
        }
    }

    /**
     * Method retrieving a file ID from a datafile if the file is located in file store
     * @param datafile data file
     * @return file ID as String
     * @throws InvalidInputException on invalid URI syntax
     */
    private String getFileIdFromDataFile(String datafile) throws InvalidInputException {
        String fileId = null;
        if(!Files.exists(Paths.get(datafile))) {
            try {
                fileId = new FileStoreUrn(datafile).getFileId();
            } catch (URISyntaxException e) {
                JobError jobError = new JobError(JobError.Code.INVALID_URI_SYNTAX, e.getMessage(), ServiceUtil.stackTraceToString(e));
                throw new InvalidInputException("Invalid file store service URI", jobError);
            }
        }
        return fileId;
    }


    /**
     * Method retrieving job data file
     *
     * @param fileId file ID, null if file is not persisted in file store
     * @return input stream
     * @throws JobStoreException on failure on retrieving job data file
     */
    private InputStream getInputStream (String dataFile, String fileId) throws JobStoreException {
        try {
            InputStream inputStream;
            if(fileId == null) {
                // The file is local
                inputStream = Files.newInputStream(Paths.get(dataFile));
            } else {
                // The file can be looked up in file store
                inputStream = fileStoreServiceConnectorBean.getConnector().getFile(fileId);
            }
            return inputStream;
        } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
            // If status is NOT_FOUND (404)
            if(e.getStatusCode() == 404) {
                final JobError jobError = new JobError(JobError.Code.INVALID_DATAFILE, e.getMessage(), ServiceUtil.stackTraceToString(e));
                throw new InvalidInputException("Job data file could not be found", jobError);
            } else {
                throw new JobStoreException("Error retrieving job data file", e);
            }
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
