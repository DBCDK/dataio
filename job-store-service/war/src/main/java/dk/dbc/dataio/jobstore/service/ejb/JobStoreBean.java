package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
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
     * Adds new job in the underlying data store from given job input stream, after attempting to retrieve
     * required referenced objects through addJobParam.
     *
     * @param jobInputStream, containing information needed to create job, chunk and item entities
     * @return information snapshot of added job
     * @throws JobStoreException on failure to add job
     */
    public JobInfoSnapshot addAndScheduleJob(JobInputStream jobInputStream) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try (AddJobParam param = new AddJobParam(jobInputStream, flowStoreServiceConnectorBean.getConnector(), fileStoreServiceConnectorBean.getConnector())) {
            JobInfoSnapshot jobInfoSnapshot = jobStore.addJob(param);
            if (!jobInfoSnapshot.getState().fatalDiagnosticExists()) {
                try {
                    compareByteSize(param.getDataFileId(), param.getDataPartitioner());
                } catch (IOException e) {
                    throw new JobStoreException("Error reading data file", e);
                }
            }
            return jobInfoSnapshot;
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
}
