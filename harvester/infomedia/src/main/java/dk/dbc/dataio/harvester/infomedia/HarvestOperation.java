/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.authornamesuggester.AuthorNameSuggesterConnector;
import dk.dbc.authornamesuggester.AuthorNameSuggesterConnectorException;
import dk.dbc.authornamesuggester.AuthorNameSuggestions;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.infomedia.model.Infomedia;
import dk.dbc.dataio.harvester.infomedia.model.Record;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import dk.dbc.infomedia.Article;
import dk.dbc.infomedia.InfomediaConnector;
import dk.dbc.infomedia.InfomediaConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);
    
    private final InfomediaHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final InfomediaConnector infomediaConnector;
    private final AuthorNameSuggesterConnector authorNameSuggesterConnector;

    public HarvestOperation(InfomediaHarvesterConfig config,
                            BinaryFileStore binaryFileStore,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            InfomediaConnector infomediaConnector,
                            AuthorNameSuggesterConnector authorNameSuggesterConnector) {
        this.config = config;
        this.binaryFileStore = binaryFileStore;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.infomediaConnector = infomediaConnector;
        this.authorNameSuggesterConnector = authorNameSuggesterConnector;
    }

    public int execute() throws HarvesterException {
        final StopWatch stopwatch = new StopWatch();
        int recordsHarvested = 0;
        try {
            try (JobBuilder jobBuilder = new JobBuilder(
                    binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                    JobSpecificationTemplate.create(config))) {

                for (Article article : getInfomediaArticles()) {
                    final Infomedia infomedia = new Infomedia();
                    infomedia.setArticle(article);
                    final Record record = new Record();
                    record.setInfomedia(infomedia);

                    if (article.getAuthors() != null) {
                        final List<AuthorNameSuggestions> authorNameSuggestions =
                                new ArrayList<>(article.getAuthors().size());
                        for (String author : article.getAuthors()) {
                            try {
                                authorNameSuggestions.add(authorNameSuggesterConnector
                                        .getSuggestions(Collections.singletonList(author)));
                            } catch (RuntimeException | AuthorNameSuggesterConnectorException e) {
                                // TODO: 16-01-19 add error to addi diagnostics
                            }
                        }
                        record.setAuthorNameSuggestions(authorNameSuggestions);
                    }

                    // TODO: 16-01-19 build addi record 
                }

                jobBuilder.build();
                recordsHarvested = jobBuilder.getRecordsAdded();
            }
            config.getContent().withTimeOfLastHarvest(new Date());
            ConfigUpdater.create(flowStoreServiceConnector).push(config);
            return recordsHarvested;
        } finally {
            LOGGER.info("Harvested {} records in {} ms",
                    recordsHarvested, stopwatch.getElapsedTime());
        }
    }

    private List<Article> getInfomediaArticles() throws HarvesterException {
        final Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        try {
            final Set<String> ids = infomediaConnector.searchArticleIds(
                    today, today, today, config.getContent().getId());
            return infomediaConnector.getArticles(ids).getArticles();
        } catch (InfomediaConnectorException e) {
            throw new HarvesterException("Unable to harvest Infomedia records", e);
        }
    }
}
