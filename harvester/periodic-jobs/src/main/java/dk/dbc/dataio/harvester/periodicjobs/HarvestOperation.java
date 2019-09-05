/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Date;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    private final PeriodicJobsHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final RawRepoConnector rawRepoConnector;
    Date timeOfSearch;

    public HarvestOperation(PeriodicJobsHarvesterConfig config,
                            BinaryFileStore binaryFileStore,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector) {
        this(config,
                binaryFileStore,
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                null);
    }

    HarvestOperation(PeriodicJobsHarvesterConfig config,
                     BinaryFileStore binaryFileStore,
                     FileStoreServiceConnector fileStoreServiceConnector,
                     FlowStoreServiceConnector flowStoreServiceConnector,
                     JobStoreServiceConnector jobStoreServiceConnector,
                     RawRepoConnector rawRepoConnector) {
        this.config = config;
        this.binaryFileStore = binaryFileStore;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.rawRepoConnector = rawRepoConnector != null
                ? rawRepoConnector
                : createRawRepoConnector(config);
    }

    /**
     * Runs this harvest operation creating a dataIO job based on record
     * IDs obtained by querying the Solr server associated with the
     * raw-repo resource
     * @return number of records harvested
     * @throws HarvesterException on failure to complete harvest operation
     */
    public int execute() throws HarvesterException {
        final String solrZkHost = getSolrZkHost();
        LOGGER.info("Using Solr zookeeper host: {}", solrZkHost);
        final BinaryFile searchResultFile = getTmpFileForSearchResult();
        try (RecordSearcher recordSearcher = createRecordSearcher(solrZkHost)) {
            final QuerySubstitutor querySubstitutor = new QuerySubstitutor();
            final String query = querySubstitutor.replace(config.getContent().getQuery(), config);
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
     * @param recordIds file where each line contains a record ID on the
     *                  form bibliographicRecordId:agencyId
     * @return number of records harvested
     * @throws HarvesterException on failure to complete harvest operation
     */
    int execute(BinaryFile recordIds) throws HarvesterException {
        // TODO: 05/09/2019 Implement record fetching
        recordIds.delete();
        return 0;
    }

    RecordSearcher createRecordSearcher(String solrZkHost) {
        return new RecordSearcher(solrZkHost);
    }

    private RawRepoConnector createRawRepoConnector(PeriodicJobsHarvesterConfig config) {
        return new RawRepoConnector(config.getContent().getResource());
    }

    private String getSolrZkHost() throws HarvesterException {
        try {
            return rawRepoConnector.getSolrZkHost();
        } catch (QueueException | SQLException | ConfigurationException e) {
            throw new HarvesterException("Unable to obtain Solr zookeeper host", e);
        }
    }

    private BinaryFile getTmpFileForSearchResult() {
        return binaryFileStore.getBinaryFile(Paths.get(config.getId() + ".record-ids.txt"));
    }
}
