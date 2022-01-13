package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.logstore.service.connector.ejb.LogStoreServiceConnectorBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static dk.dbc.dataio.commons.types.JobSpecification.JOB_EXPIRATION_AGE_IN_DAYS;

/**
 * This enterprise Java bean handles and schedules job purge.
 *
 * A job purge includes deletion of job in job store and deletion of all log entries.
 * If the original data file is used only by the job to be deleted, the data file is deleted from
 * file store as well.
 *
 * Jobs of type ACCTEST are deleted 5 days after time of creation
 * Jobs of type TRANSIENT and TEST are deleted 90 days after time of creation
 * Jobs of type PERSISTENT are not deleted
 * Jobs of type INFOMEDIA are deleted 14 days after time of creation
 * Jobs of type PERIODIC are deleted 180 days after time of creation
 */
@Singleton
public class JobPurgeBean {


    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @EJB
    PgJobStoreRepository pgJobStoreRepository;

    @EJB
    LogStoreServiceConnectorBean logStoreServiceConnectorBean;

    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @Resource
    SessionContext sessionContext;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobPurgeBean.class);

    @Stopwatch
    public void purgeJobs() throws FileStoreServiceConnectorException, LogStoreServiceConnectorUnexpectedStatusCodeException {
        final List<JobInfoSnapshot> jobCandidates = getJobsForDeletion();
        LOGGER.info("starting scheduled job purge for {} jobs", jobCandidates.size());
        for (JobInfoSnapshot jobInfoSnapshot : jobCandidates) {
            self().delete(jobInfoSnapshot);
        }

        // Compact jobs older than 1810 days (appx five years).
        final List<JobInfoSnapshot> veryOldPersistentJobs = getJobsToCompact(JobSpecification.Type.PERSISTENT,
                JOB_EXPIRATION_AGE_IN_DAYS, ChronoUnit.DAYS);
        LOGGER.info("Compacting {} jobs older than three years", veryOldPersistentJobs.size());
        for (JobInfoSnapshot jobInfoSnapshot : veryOldPersistentJobs) {
            self().compact(jobInfoSnapshot);
        }
    }

    /**
     * Deletes the job in job store as well as any log entries belonging to the job.
     * If the original data file is used only by the job to be deleted, the data file is deleted from
     * file store as well.
     * @param jobInfoSnapshot representing the job to delete
     * @throws LogStoreServiceConnectorUnexpectedStatusCodeException on failure while deleting job logs
     * @throws FileStoreServiceConnectorException on failure while deleting the datafile
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void delete(JobInfoSnapshot jobInfoSnapshot) throws LogStoreServiceConnectorUnexpectedStatusCodeException, FileStoreServiceConnectorException {
        LOGGER.info("purging job {} of type {} from {}", jobInfoSnapshot.getJobId(),
                jobInfoSnapshot.getSpecification().getType(), jobInfoSnapshot.getTimeOfCreation());

        LOGGER.info("purging log-store entries for job {}", jobInfoSnapshot.getJobId());
        logStoreServiceConnectorBean.getConnector().deleteJobLogs(String.valueOf(jobInfoSnapshot.getJobId()));

        // Delete data file only if not used by other jobs
        final String dataFile = jobInfoSnapshot.getSpecification().getDataFile();
        if (numberOfJobsUsingDatafile(dataFile) == 1) {
            try {
                LOGGER.info("purging file-store entry {} for job {}", dataFile, jobInfoSnapshot.getJobId());
                fileStoreServiceConnectorBean.getConnector().deleteFile(FileStoreUrn.parse(dataFile));
            } catch (IllegalArgumentException | NullPointerException e) {
                LOGGER.error("invalid file-store URN {} for job {}", dataFile, jobInfoSnapshot.getJobId());
            } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
                if (e.getStatusCode() == Response.Status.NOT_FOUND.getStatusCode()) {
                    LOGGER.error("data file {} not found for job {}", dataFile, jobInfoSnapshot.getJobId());
                } else {
                    throw e;
                }
            }
        }

        // Delete the jobEntity
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        entityManager.remove(jobEntity);
    }

    /**
     *
     * @param jobInfoSnapshot description of the job to delete
     * @throws LogStoreServiceConnectorUnexpectedStatusCodeException on failure to connect to logstore
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void compact(JobInfoSnapshot jobInfoSnapshot) throws LogStoreServiceConnectorUnexpectedStatusCodeException {
        LOGGER.info("Compacting job {} of type {} that did complete at {}", jobInfoSnapshot.getJobId(),
                jobInfoSnapshot.getSpecification().getType(), jobInfoSnapshot.getTimeOfCompletion());
        LOGGER.info("Purging log-store entries for job {}", jobInfoSnapshot.getJobId());
        logStoreServiceConnectorBean.getConnector().deleteJobLogs(String.valueOf(jobInfoSnapshot.getJobId()));

        Query query = entityManager.createQuery("select i from ItemEntity i where i.key.jobId = :jobid");
        query.setParameter("jobid", jobInfoSnapshot.getJobId());
        LOGGER.info("Removing items for job {}", jobInfoSnapshot.getJobId());
        final List<ItemEntity> itemEntities = query.getResultList();
        for (ItemEntity itemEntity : itemEntities) {
            entityManager.remove(itemEntity);
        }

        query = entityManager.createQuery("select c from ChunkEntity c where c.key.jobId = :jobid");
        query.setParameter("jobid", jobInfoSnapshot.getJobId());
        LOGGER.info("Removing chunks for job {}", jobInfoSnapshot.getJobId());
        final List<ChunkEntity> chunkEntities = query.getResultList();
        for (ChunkEntity chunkEntity : chunkEntities) {
            entityManager.remove(chunkEntity);
        }

       final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
       jobEntity.setNumberOfItems(0);
       jobEntity.setNumberOfChunks(0);
       JobSpecification jobSpecification = JobSpecification.from(jobEntity.getSpecification());
       jobSpecification.withType(JobSpecification.Type.COMPACTED);
       jobEntity.setSpecification(jobSpecification);
    }

    /**
     * creates a list of job deletion candidates
     * @return the list of jobs candidates for deletion
     */
    private List<JobInfoSnapshot> getJobsForDeletion() {
        final List<JobInfoSnapshot> toDelete = new ArrayList<>();
        toDelete.addAll(getJobsForDeletion(JobSpecification.Type.ACCTEST,5));
        toDelete.addAll(getJobsForDeletion(JobSpecification.Type.TEST, 90));
        toDelete.addAll(getJobsForDeletion(JobSpecification.Type.TRANSIENT, 90));
        toDelete.addAll(getJobsForDeletion(JobSpecification.Type.INFOMEDIA, 14));
        toDelete.addAll(getJobsForDeletion(JobSpecification.Type.PERIODIC, 180));
        return toDelete;
    }

    /**
     * Retrieves jobs that matches given type and has a creation date
     * earlier than current date minus the number of days given as input
     * @param type of job
     * @param days defining how far back the search goes
     * @return list of jobs that matched the criteria
     */
    private List<JobInfoSnapshot> getJobsForDeletion(JobSpecification.Type type, int days) {
        return getJobsForDeletion(type, days, ChronoUnit.DAYS);
    }

    /* Class scoped due to test */
    List<JobInfoSnapshot> getJobsForDeletion(JobSpecification.Type type, int delta, TemporalUnit chronoUnit ) {
        final Date marker = Date.from(Instant.now().minus(delta, chronoUnit));
        final JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"" + type.name() + "\"}"))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NOT_NULL))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.LESS_THAN, marker));
        return pgJobStoreRepository.listJobs(criteria);
    }

    /**
     * Retrieves a list of jobs to be "compacted".
     * @param type of job
     * @param delta how long back to look
     * @param chronoUnit days, seconds, etc
     * @return list of jobs scheduled for compact
     */
    List<JobInfoSnapshot> getJobsToCompact(JobSpecification.Type type, int delta, TemporalUnit chronoUnit ) {
        final Date marker = Date.from(Instant.now().minus(delta, chronoUnit));
        final JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"" + type.name() + "\"}"))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NOT_NULL))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.LESS_THAN, marker));

        List<JobInfoSnapshot> result = pgJobStoreRepository.listJobs(criteria).stream().filter(jobInfoSnapshot ->
                jobInfoSnapshot.getNumberOfItems() > 0 && jobInfoSnapshot.getNumberOfChunks() > 0)
                .collect(Collectors.toList());

        // Narrow list to only feature jobs that actually has existing items (we do not want those already compacted).
        return result.stream().filter(jobInfoSnapshot -> pgJobStoreRepository
                .countItems(new ItemListCriteria()
                        .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID,
                                ListFilter.Op.EQUAL, jobInfoSnapshot.getJobId()))) > 0)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the number of jobs referencing the provided data file
     * @param datafile to search for
     * @return the number of jobs referencing the provided data file
     */
    private long numberOfJobsUsingDatafile(String datafile) {
        final JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"dataFile\": \"" + datafile + "\"}"));
        return pgJobStoreRepository.countJobs(criteria);
    }

    /* Securing individual transactions */
    private JobPurgeBean self() {
        return sessionContext.getBusinessObject(JobPurgeBean.class);
    }
}
