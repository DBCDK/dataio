package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.macroexpansion.MacroSubstitutor;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorFactory;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import dk.dbc.weekresolver.connector.WeekResolverConnectorException;
import jakarta.ws.rs.ProcessingException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    static int MAX_NUMBER_OF_TASKS = 10;

    protected PeriodicJobsHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final WeekResolverConnector weekResolverConnector;
    private final ExecutorService executor;
    private final RawRepoConnector rawRepoConnector;
    Date timeOfSearch;

    public HarvestOperation(PeriodicJobsHarvesterConfig config,
                            BinaryFileStore binaryFileStore,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            WeekResolverConnector weekResolverConnector,
                            ExecutorService executor) {
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
                     ExecutorService executor,
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
        BinaryFile searchResultFile = getTmpFileForSearchResult();
        return execute(searchAndPersist(searchResultFile));
    }

    protected BinaryFile searchAndPersist(BinaryFile searchResultFile) throws HarvesterException {
        try (RecordSearcher recordSearcher = createRecordSearcher()) {
            for (String queryString : getQueries()) {
                MacroSubstitutor macroSubstitutor = new MacroSubstitutor(this::catalogueCodeToWeekCode)
                        .addUTC("__TIME_OF_LAST_HARVEST__", config.getContent().getTimeOfLastHarvest());
                String query = macroSubstitutor.replace(queryString);
                LOGGER.info("Executing Solr query (rawrepo): {}", query);
                long numberOfDocsFound = recordSearcher.search(
                        config.getContent().getCollection(), query, searchResultFile);
                LOGGER.info("Solr query found {} documents", numberOfDocsFound);
                this.timeOfSearch = macroSubstitutor.getNow();
            }
        }
        return searchResultFile;
    }

    protected List<String> search() throws HarvesterException {
        List<String> result = new ArrayList<>();
        try (RecordSearcher recordSearcher = createRecordSearcher()) {
            for (String queryString : getQueries()) {
                MacroSubstitutor macroSubstitutor = new MacroSubstitutor(this::catalogueCodeToWeekCode)
                        .addUTC("__TIME_OF_LAST_HARVEST__", config.getContent().getTimeOfLastHarvest());
                String query = macroSubstitutor.replace(queryString);
                LOGGER.info("Executing Solr query (rawrepo): {}", query);
                List<String> tempResult = recordSearcher.search(config.getContent().getCollection(), query);
                LOGGER.info("Solr query found {} documents", tempResult.size());
                result.addAll(tempResult.stream().map(s -> s.split(":").length == 0 ? s : s.split(":")[0] ).collect(Collectors.toList()));
                this.timeOfSearch = macroSubstitutor.getNow();
            }
        }
        return result;
    }

    /**
     * Runs the solr query and returns a text describing the query and the result
     *
     * @return number of record IDs found
     * @throws HarvesterException on failure to complete harvest operation
     */
    public String validateQuery() throws HarvesterException {
        String status;
        StringBuilder result = new StringBuilder();
        result.append("Solr søgning: ")
                .append("\n");
        List<String> queries = getQueries();
        int numberOfResultsTotal = 0;

        try (RecordSearcher recordSearcher = createRecordSearcher()) {
            for (String queryString : queries) {
                MacroSubstitutor macroSubstitutor = new MacroSubstitutor(this::catalogueCodeToWeekCode)
                        .addUTC("__TIME_OF_LAST_HARVEST__", config.getContent().getTimeOfLastHarvest());
                String query = macroSubstitutor.replace(queryString);
                result.append(query)
                        .append("\n");
                numberOfResultsTotal += recordSearcher.validate(
                        config.getContent().getCollection(), query);
                this.timeOfSearch = macroSubstitutor.getNow();
            }

            status = "Status: OK\n\n";
            result.append("\n");
            result.append("Antal fundne post-id'er: ")
                    .append(numberOfResultsTotal);
        } catch (SolrServerException | HttpSolrClient.RemoteSolrException e) {
            status = "Status: FEJL\n\n";
            result.append("\n");
            result.append(e.getMessage());
        }

        return status + result;
    }

    protected List<String> getQueries() throws HarvesterException {
        List<String> queries = new ArrayList<>();

        if (config.getContent().getQueryFileId() != null) {
            LOGGER.info("Found FileStore id {} so using file", config.getContent().getQueryFileId());
            String fileId = config.getContent().getQueryFileId();
            try (InputStream queryFileInputStream = fileStoreServiceConnector.getFile(fileId);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(queryFileInputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    queries.add(line);
                }
            } catch (FileStoreServiceConnectorException | IOException e) {
                throw new HarvesterException("Failed to get file from FileStore", e);
            }
        } else {
            LOGGER.info("Using query fron config");
            queries.add(config.getContent().getQuery());
        }

        return queries;
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
            Iterator<RecordIdDTO> recordIdsIterator = recordIdFile.iterator();
            if (recordIdsIterator.hasNext()) {
                List<RecordFetcher> fetchRecordTasks;
                do {
                    fetchRecordTasks = getNextTasks(recordIdsIterator, recordServiceConnector, MAX_NUMBER_OF_TASKS);
                    List<Future<AddiRecord>> addiRecords = executor.invokeAll(fetchRecordTasks);
                    for (Future<AddiRecord> addiRecordFuture : addiRecords) {
                        AddiRecord addiRecord = addiRecordFuture.get();
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
            LOGGER.error("Harvest operation exception:", e);
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
            String solrZkHost = rawRepoConnector.getSolrZkHost();
            LOGGER.info("Using Solr zookeeper host: {}", solrZkHost);
            return new RecordSearcher(solrZkHost);
        } catch (QueueException | SQLException | ConfigurationException e) {
            throw new HarvesterException("Unable to obtain Solr zookeeper host", e);
        }
    }

    RecordServiceConnector createRecordServiceConnector() throws HarvesterException {
        try {
            String recordServiceUrl = rawRepoConnector.getRecordServiceUrl();
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
        JobSpecification jobSpecification = jobBuilder.createJobSpecification(
                FileStoreUrn.EMPTY_JOB_FILE.getFileId());
        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addEmptyJob(
                new JobInputStream(jobSpecification, true, 0));
        LOGGER.info("Created empty job in job-store with ID {}", jobInfoSnapshot.getJobId());
    }

    protected BinaryFile getTmpFileForSearchResult() {
        BinaryFile binaryFile = binaryFileStore.getBinaryFile(
                Paths.get(config.getId() + ".record-ids.txt"));
        if (binaryFile.exists()) {
            binaryFile.delete();
        }
        return binaryFile;
    }

    private List<RecordFetcher> getNextTasks(Iterator<RecordIdDTO> recordIdsIterator,
                                             RecordServiceConnector recordServiceConnector, int maxNumberOfTasks) {
        List<RecordFetcher> tasks = new ArrayList<>(maxNumberOfTasks);
        while (recordIdsIterator.hasNext() && tasks.size() < maxNumberOfTasks) {
            RecordIdDTO recordId = recordIdsIterator.next();
            if (recordId != null) {
                tasks.add(getRecordFetcher(recordId, recordServiceConnector, config));
            }
        }
        return tasks;
    }

    RecordFetcher getRecordFetcher(RecordIdDTO recordId, RecordServiceConnector recordServiceConnector,
                                   PeriodicJobsHarvesterConfig config) {
        return new RecordFetcher(recordId, recordServiceConnector, config);
    }

    private void updateConfigWithTimeOfSearch() throws HarvesterException {
        config.getContent()
                .withTimeOfLastHarvest(timeOfSearch);
        ConfigUpdater.create(flowStoreServiceConnector).push(config);
    }

    public String catalogueCodeToWeekCode(String catalogueCode, LocalDate localDate) {
        try {
            return weekResolverConnector.getCurrentWeekCodeForDate(catalogueCode, localDate).getWeekCode();
        } catch (WeekResolverConnectorException e) {
            throw new ProcessingException(e);
        }
    }
}
