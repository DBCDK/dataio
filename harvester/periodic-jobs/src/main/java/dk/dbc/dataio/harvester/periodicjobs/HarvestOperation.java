/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorFactory;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.weekresolver.WeekResolverConnector;
import dk.dbc.weekresolver.WeekResolverConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.ws.rs.ProcessingException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    static int MAX_NUMBER_OF_TASKS = 10;

    private final PeriodicJobsHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final WeekResolverConnector weekResolverConnector;
    private final ManagedExecutorService executor;
    private final RawRepoConnector rawRepoConnector;
    Date timeOfSearch;

    public HarvestOperation(PeriodicJobsHarvesterConfig config,
                            BinaryFileStore binaryFileStore,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            WeekResolverConnector weekResolverConnector,
                            ManagedExecutorService executor) {
        this(config,
                binaryFileStore,
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                weekResolverConnector,
                executor,
                null);
    }

    HarvestOperation(PeriodicJobsHarvesterConfig config,
                     BinaryFileStore binaryFileStore,
                     FileStoreServiceConnector fileStoreServiceConnector,
                     FlowStoreServiceConnector flowStoreServiceConnector,
                     JobStoreServiceConnector jobStoreServiceConnector,
                     WeekResolverConnector weekResolverConnector,
                     ManagedExecutorService executor,
                     RawRepoConnector rawRepoConnector) {
        this.config = config;
        this.binaryFileStore = binaryFileStore;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.weekResolverConnector = weekResolverConnector;
        this.executor = executor;
        this.rawRepoConnector = rawRepoConnector != null
                ? rawRepoConnector
                : createRawRepoConnector(config);
    }

    /**
     * Runs this harvest operation creating a dataIO job based on record
     * IDs obtained by querying the Solr server associated with the
     * raw-repo resource
     *
     * @return number of records harvested
     * @throws HarvesterException on failure to complete harvest operation
     */
    public int execute() throws HarvesterException {
        final BinaryFile searchResultFile = getTmpFileForSearchResult();
        try (RecordSearcher recordSearcher = createRecordSearcher()) {
            final QuerySubstitutor querySubstitutor = new QuerySubstitutor();
            final String query = querySubstitutor.replace(config.getContent().getQuery(), config,
                    this::catalogueCodeToWeekCode);
            LOGGER.info("Executing Solr query: {}", query);
            final long numberOfDocsFound = recordSearcher.search(
                    config.getContent().getCollection(), query, searchResultFile);
            LOGGER.info("Solr query found {} documents", numberOfDocsFound);
            this.timeOfSearch = querySubstitutor.getNow();
        }
        return execute(searchResultFile);
    }

    /**
     * Runs this harvest operation creating a dataIO job based on record
     * IDs contained in the given file
     * <p>
     * If any non-internal error occurs a record is marked as failed.
     * </p>
     *
     * @param recordIds file where each line contains a record ID on the
     *                  form bibliographicRecordId:agencyId
     * @return number of records harvested
     * @throws HarvesterException on failure to complete harvest operation
     */
    int execute(BinaryFile recordIds) throws HarvesterException {
        RecordServiceConnector recordServiceConnector = null;
        try (RecordIdFile recordIdFile = new RecordIdFile(recordIds);
             JobBuilder jobBuilder = createJobBuilder()) {
            recordServiceConnector = createRecordServiceConnector();
            final Iterator<RecordId> recordIdsIterator = recordIdFile.iterator();
            if (recordIdsIterator.hasNext()) {
                List<RecordFetcher> fetchRecordTasks;
                do {
                    fetchRecordTasks = getNextTasks(recordIdsIterator, recordServiceConnector, MAX_NUMBER_OF_TASKS);
                    final List<Future<AddiRecord>> addiRecords = executor.invokeAll(fetchRecordTasks);
                    for (Future<AddiRecord> addiRecordFuture : addiRecords) {
                        final AddiRecord addiRecord = addiRecordFuture.get();
                        if (addiRecord != null) {
                            jobBuilder.addRecord(addiRecordFuture.get());
                        }
                    }
                } while (!fetchRecordTasks.isEmpty());

                jobBuilder.build();
            } else {
                // Query found zero record IDs so an empty job is created
                createEmptyJob(jobBuilder);
            }

            if (timeOfSearch != null) {
                updateConfigWithTimeOfSearch();
            }

            return jobBuilder.getRecordsAdded();
        } catch (InterruptedException | ExecutionException | JobStoreServiceConnectorException e) {
            throw new HarvesterException("Unable to complete harvest operation", e);
        } finally {
            recordIds.delete();
            if (recordServiceConnector != null) {
                recordServiceConnector.close();
            }
        }
    }

    private RawRepoConnector createRawRepoConnector(PeriodicJobsHarvesterConfig config) {
        return new RawRepoConnector(config.getContent().getResource());
    }

    RecordSearcher createRecordSearcher() throws HarvesterException {
        try {
            final String solrZkHost = rawRepoConnector.getSolrZkHost();
            LOGGER.info("Using Solr zookeeper host: {}", solrZkHost);
            return new RecordSearcher(solrZkHost);
        } catch (QueueException | SQLException | ConfigurationException e) {
            throw new HarvesterException("Unable to obtain Solr zookeeper host", e);
        }
    }

    RecordServiceConnector createRecordServiceConnector() throws HarvesterException {
        try {
            final String recordServiceUrl = rawRepoConnector.getRecordServiceUrl();
            LOGGER.info("Using record service URL: {}", recordServiceUrl);
            return RecordServiceConnectorFactory.create(recordServiceUrl);
        } catch (SQLException | QueueException | ConfigurationException e) {
            throw new HarvesterException("Unable to obtain record service URL", e);
        }
    }

    JobBuilder createJobBuilder() throws HarvesterException {
        return new JobBuilder(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                JobSpecificationTemplate.create(config));
    }

    void createEmptyJob(JobBuilder jobBuilder) throws JobStoreServiceConnectorException {
        final JobSpecification jobSpecification = jobBuilder.createJobSpecification(
                FileStoreUrn.EMPTY_JOB_FILE.getFileId());
        final JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addEmptyJob(
                new JobInputStream(jobSpecification, true, 0));
        LOGGER.info("Created empty job in job-store with ID {}", jobInfoSnapshot.getJobId());
    }

    private BinaryFile getTmpFileForSearchResult() {
        final BinaryFile binaryFile = binaryFileStore.getBinaryFile(
                Paths.get(config.getId() + ".record-ids.txt"));
        if (binaryFile.exists()) {
            binaryFile.delete();
        }
        return binaryFile;
    }

    private List<RecordFetcher> getNextTasks(Iterator<RecordId> recordIdsIterator,
                                             RecordServiceConnector recordServiceConnector, int maxNumberOfTasks) {
        final List<RecordFetcher> tasks = new ArrayList<>(maxNumberOfTasks);
        while (recordIdsIterator.hasNext() && tasks.size() < maxNumberOfTasks) {
            final RecordId recordId = recordIdsIterator.next();
            if (recordId != null) {
                tasks.add(getRecordFetcher(recordId, recordServiceConnector, config));
            }
        }
        return tasks;
    }

    RecordFetcher getRecordFetcher(RecordId recordId, RecordServiceConnector recordServiceConnector,
                                   PeriodicJobsHarvesterConfig config) {
        return new RecordFetcher(recordId, recordServiceConnector, config);
    }

    private void updateConfigWithTimeOfSearch() throws HarvesterException {
        config.getContent()
                .withTimeOfLastHarvest(timeOfSearch);
        ConfigUpdater.create(flowStoreServiceConnector).push(config);
    }

    private String catalogueCodeToWeekCode(String catalogueCode, LocalDate localDate) {
        try {
            return weekResolverConnector.getWeekCode(catalogueCode, localDate).getWeekCode();
        } catch (WeekResolverConnectorException e) {
            throw new ProcessingException(e);
        }
    }
}
