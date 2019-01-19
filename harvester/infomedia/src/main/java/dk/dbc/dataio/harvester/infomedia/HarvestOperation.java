/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.infomedia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dk.dbc.authornamesuggester.AuthorNameSuggesterConnector;
import dk.dbc.authornamesuggester.AuthorNameSuggesterConnectorException;
import dk.dbc.authornamesuggester.AuthorNameSuggestions;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.infomedia.model.Infomedia;
import dk.dbc.dataio.harvester.infomedia.model.Record;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.infomedia.Article;
import dk.dbc.infomedia.InfomediaConnector;
import dk.dbc.infomedia.InfomediaConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
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
    private final JSONBContext jsonbContext = new JSONBContext();
    private final XmlMapper xmlMapper = new XmlMapper();

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
                    LOGGER.info("{} ready for harvesting", article.getArticleId());

                    final AddiMetaData addiMetaData = createAddiMetaData(article);

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
                                final String errMsg = String.format(
                                        "Getting author name suggestions failed for %s: %s",
                                        author, e.getMessage());
                                addiMetaData.withDiagnostic(new Diagnostic(
                                        Diagnostic.Level.FATAL, errMsg));
                                LOGGER.error(errMsg, e);
                            }
                        }
                        record.setAuthorNameSuggestions(authorNameSuggestions);
                    }

                    jobBuilder.addRecord(createAddiRecord(addiMetaData, record));
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

    private AddiMetaData createAddiMetaData(Article article) {
        return new AddiMetaData()
                .withTrackingId("Infomedia." + config.getLogId() + "."
                        + article.getArticleId())
                .withBibliographicRecordId(article.getArticleId())
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat(config.getContent().getFormat())
                .withCreationDate(getCreationDate(article))
                .withLibraryRules(new AddiMetaData.LibraryRules());
    }

    private Date getCreationDate(Article article) {
        try {
            if (article.getPublishDate() != null) {
                return Date.from(Instant.parse(article.getPublishDate()));
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to get creation date from {}", article);
        }
        return null;
    }

    private AddiRecord createAddiRecord(AddiMetaData metaData, Record record) throws HarvesterException {
        try {
            return new AddiRecord(
                    jsonbContext.marshall(metaData).getBytes(StandardCharsets.UTF_8),
                    xmlMapper.writeValueAsString(record).getBytes(StandardCharsets.UTF_8));
        } catch (JSONBException | JsonProcessingException e) {
            throw new HarvesterException(e);
        }
    }
}
