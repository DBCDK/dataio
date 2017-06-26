package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by sma on 12-06-17.
 * This enterprise Java bean handles and schedules job purge.
 *
 * A job purge includes deletion of job in job store and deletion of all log entries.
 * If the original data file is used only by the job to be deleted, the data file is deleted from
 * file store as well.
 *
 * Jobs of type ACCTEST are deleted 1 month after time of creation
 * Jobs of types TRANSIENT and TEST are deleted 3 months after time of creation
 * Jobs of types PERSISTENT are not deleted
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
        LOGGER.info("starting scheduled job purge for '{}' jobs", jobCandidates.size());
        for (JobInfoSnapshot jobInfoSnapshot : jobCandidates) {
            self().delete(jobInfoSnapshot);
        }
    }

    /**
     * Deletes the job in job store as well as any log entries belonging to the job.
     * If the original data file is used only by the job to be deleted, the data file is deleted from
     * file store as well.
     * @param jobInfoSnapshot representing the job to delete
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    void delete(JobInfoSnapshot jobInfoSnapshot) throws LogStoreServiceConnectorUnexpectedStatusCodeException, FileStoreServiceConnectorException {
        // Delete all log entries for given job
        logStoreServiceConnectorBean.getConnector().deleteJobLogs(String.valueOf(jobInfoSnapshot.getJobId()));

        // Delete data file only if not used by other jobs
        final String dataFile = jobInfoSnapshot.getSpecification().getDataFile();
        if(numberOfJobsUsingDatafile(dataFile) == 1) {
            fileStoreServiceConnectorBean.getConnector().deleteFile(dataFile);
        }

        // Delete the jobEntity
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        entityManager.remove(jobEntity);
    }

    /**
     * creates a list of job deletion candidates
     * @return the list of jobs candidates for deletion
     */
    private List<JobInfoSnapshot> getJobsForDeletion() {
        final List<JobInfoSnapshot> toDelete = new ArrayList<>();
        toDelete.addAll(getJobsForDeletion(JobSpecification.Type.ACCTEST,30));      // 1 month
        toDelete.addAll(getJobsForDeletion(JobSpecification.Type.TEST, 90));        // 3 months
        toDelete.addAll(getJobsForDeletion(JobSpecification.Type.TRANSIENT, 90));   // 3 months
        return toDelete;
    }

    /**
     * Retrieves jobs that matches given type and has a creation date earlier than current date minus the number of months given as input
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
