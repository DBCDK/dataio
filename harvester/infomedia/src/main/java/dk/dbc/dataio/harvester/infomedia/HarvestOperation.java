/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.infomedia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import dk.dbc.autonomen.AutoNomenConnector;
import dk.dbc.autonomen.AutoNomenConnectorException;
import dk.dbc.autonomen.AutoNomenSuggestions;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.TimeInterval;
import dk.dbc.dataio.harvester.TimeIntervalGenerator;
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

    /* To support the feature that the harvester can be turned back
       in time publication date wise, a nextPublicationDate field
       was added to the configuration.

       If nextPublicationDate is not set the harvester will use
       today as publication date.

       If nextPublicationDate is set to today or a value in the past
       the harvester will harvest each publication date from the
       configured valued up to and including today.

       If nextPublicationDate is set to a date in the future the
       harvester will harvest today. (This is to support the case
       where a publication date is scheduled to be harvested more
       than once on the same day. Each successful execute() call
       will therefore set nextPublicationDate to tomorrow).
     */

    static List<Instant> getPublicationDatesToHarvest(InfomediaHarvesterConfig config) {
        Instant from = Instant.now();
        if (config.getContent().getNextPublicationDate() != null) {
            from = config.getContent().getNextPublicationDate().toInstant();
            if (from.isAfter(Instant.now())) {
                // Configured next publication date points to the
                // future, so we harvest today instead.
                from = Instant.now();
            }
        }
        from = from.truncatedTo(ChronoUnit.DAYS);

        final List<Instant> publicationDates = new ArrayList<>();
        final TimeIntervalGenerator timeIntervalGenerator = new TimeIntervalGenerator()
                .withStartingPoint(from)
                .withEndPoint(Instant.now(), 0, ChronoUnit.DAYS)
                .withIntervalDuration(1, ChronoUnit.DAYS);
        for (TimeInterval timeInterval : timeIntervalGenerator) {
            publicationDates.add(timeInterval.getFrom().truncatedTo(ChronoUnit.DAYS));
        }
        return publicationDates;
    }

    private final InfomediaHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final InfomediaConnector infomediaConnector;
    private final AutoNomenConnector autoNomenConnector;
    private final JSONBContext jsonbContext = new JSONBContext();
    private final XmlMapper xmlMapper = new XmlMapper();

    public HarvestOperation(InfomediaHarvesterConfig config,
                            BinaryFileStore binaryFileStore,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            InfomediaConnector infomediaConnector,
                            AutoNomenConnector autoNomenConnector) {
        this.config = config;
        this.binaryFileStore = binaryFileStore;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.infomediaConnector = infomediaConnector;
        this.autoNomenConnector = autoNomenConnector;
    }

    public int execute() throws HarvesterException {
        final StopWatch stopwatch = new StopWatch();
        int recordsHarvested = 0;
        try {
            final List<Instant> publicationDates = getPublicationDatesToHarvest(config);
            for (Instant publicationDate : publicationDates) {
                LOGGER.info("Harvesting publication date {}", publicationDate);

                try (JobBuilder jobBuilder = new JobBuilder(
                        binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                        JobSpecificationTemplate.create(config))) {

                    for (Article article : getInfomediaArticles(publicationDate)) {
                        LOGGER.info("{} ready for harvesting", article.getArticleId());

                        final AddiMetaData addiMetaData = createAddiMetaData(article);

                        final Infomedia infomedia = new Infomedia();
                        infomedia.setArticle(article);
                        final Record record = new Record();
                        record.setInfomedia(infomedia);

                        try {
                            final AutoNomenSuggestions autoNomenSuggestions = autoNomenConnector
                                    .getSuggestions(article.getArticleId());
                            if (!(autoNomenSuggestions.getAutNames().isEmpty() &&
                                    autoNomenSuggestions.getNerNames().isEmpty())) {
                                record.setAutoNomenSuggestions(Collections.singletonList(autoNomenSuggestions));
                            }
                        } catch (RuntimeException | AutoNomenConnectorException e) {
                            final String errMsg = String.format(
                                    "Getting author name suggestions failed for article %s: %s",
                                    article.getArticleId(), e.getMessage());
                            addiMetaData.withDiagnostic(new Diagnostic(
                                    Diagnostic.Level.FATAL, errMsg));
                            LOGGER.error(errMsg, e);
                        }

                        jobBuilder.addRecord(createAddiRecord(addiMetaData, record));
                    }

                    jobBuilder.build();
                    recordsHarvested = jobBuilder.getRecordsAdded();
                }
            }
            config.getContent()
                    .withTimeOfLastHarvest(new Date())
                    .withNextPublicationDate(Date.from(Instant.now()
                            .plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)));
            ConfigUpdater.create(flowStoreServiceConnector).push(config);
            return recordsHarvested;
        } finally {
            LOGGER.info("Harvested {} records in {} ms",
                    recordsHarvested, stopwatch.getElapsedTime());
        }
    }

    private List<Article> getInfomediaArticles(Instant publicationDate) throws HarvesterException {
        try {
            final Set<String> ids = infomediaConnector.searchArticleIds(
                    publicationDate, publicationDate, publicationDate, config.getContent().getId());
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
