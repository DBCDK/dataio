package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.creatordetector.connector.CreatorDetectorConnector;
import dk.dbc.dataio.commons.creatordetector.connector.CreatorDetectorConnectorException;
import dk.dbc.dataio.commons.creatordetector.connector.CreatorNameSuggestion;
import dk.dbc.dataio.commons.creatordetector.connector.CreatorNameSuggestions;
import dk.dbc.dataio.commons.creatordetector.connector.DetectCreatorNamesRequest;
import dk.dbc.dataio.commons.retriever.connector.RetrieverConnector;
import dk.dbc.dataio.commons.retriever.connector.RetrieverConnectorException;
import dk.dbc.dataio.commons.retriever.connector.model.Article;
import dk.dbc.dataio.commons.retriever.connector.model.ArticlesRequest;
import dk.dbc.dataio.commons.retriever.connector.model.ArticlesResponse;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

/**
 * Orchestrates the harvesting operation for Infomedia articles from the retriever platform.
 * <p>
 * This class manages the complete workflow of fetching articles with a retriever connector,
 * processing them with metadata enrichment including creator name suggestions, and building
 * jobs for submission to the job store. The operation handles multiple publication dates
 * in a single execution cycle and supports temporal back-tracking for re-harvesting historical
 * publication dates.
 * <p>
 * The harvester operates with configurable timezone awareness, defaulting to Europe/Copenhagen
 * if no TZ environment variable is set. Publication dates are determined based on the
 * configuration's nextPublicationDate field, allowing the harvester to process dates from
 * the configured date up to and including today.
 * <p>
 * The operation integrates with multiple external services:
 * - Retriever platform for fetching articles
 * - Creator-Detector service for enriching articles with creator name suggestions
 * <p>
 * Each harvested article is transformed into an ADDI record with associated metadata including
 * tracking information, bibliographic record identifiers, and diagnostic information if
 * processing errors occur.
 * <p>
 * Upon successful completion, the operation updates the harvester configuration with the
 * current harvest timestamp and sets the next publication date to tomorrow, preparing
 * for subsequent harvest cycles.
 * <p>
 * Thread safety: This class is not thread-safe. Instances should not be shared across
 * threads without external synchronization.
 */
public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);
    private static final int RETRIEVER_DEFAULT_PAGE_SIZE = 100;

    private static Supplier<ZoneId> timezoneSupplier = HarvestOperation::resolveTimezone;

    public static ZoneId getTimezone() {
        return timezoneSupplier.get();
    }

    private static ZoneId resolveTimezone() {
        String tzEnv = System.getenv("TZ");
        if (tzEnv == null) {
            tzEnv = "Europe/Copenhagen";
        }
        return ZoneId.of(tzEnv);
    }

    private static Supplier<Integer> retrieverPageSizeSupplier = () -> RETRIEVER_DEFAULT_PAGE_SIZE;

    public static int getRetrieverPageSize() {
        return retrieverPageSizeSupplier.get();
    }

    /**
     * Fallback date-time formatter for parsing publishing dates in ISO-8601 format without timezone information.
     * Uses the pattern "yyyy-MM-dd'T'HH:mm:ss" to parse dates when the primary parsing mechanism fails.
     * This formatter is used as a secondary parsing strategy to handle articles with publishing dates
     * that do not include timezone offset information.
     */
    private static final DateTimeFormatter PUBLISHING_DATE_FALLBACK_PARSER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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

    static List<LocalDate> getPublicationDatesToHarvest(InfomediaHarvesterConfig config) {
        final ZoneId zoneId = getTimezone();
        final LocalDate today = LocalDate.now(zoneId);

        LocalDate fromDate = today;
        if (config.getContent().getNextPublicationDate() != null) {
            fromDate = config.getContent().getNextPublicationDate().toInstant()
                    .atZone(zoneId)
                    .toLocalDate();

            if (fromDate.isAfter(today)) {
                // Configured next publication date points to the future,
                // so we harvest today instead.
                fromDate = today;
            }
        }

        final List<LocalDate> publicationDates = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(today); date = date.plusDays(1)) {
            publicationDates.add(date);
        }

        return publicationDates;
    }

    static void setTimezoneSupplierForTests(Supplier<ZoneId> supplier) {
        timezoneSupplier = supplier != null ? supplier : HarvestOperation::resolveTimezone;
    }

    static void setRetrieverPageSizeSupplierForTests(Supplier<Integer> supplier) {
        retrieverPageSizeSupplier = supplier != null ? supplier : () -> RETRIEVER_DEFAULT_PAGE_SIZE;
    }

    private final InfomediaHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final RetrieverConnector retrieverConnector;
    private final CreatorDetectorConnector creatorDetectorConnector;
    private final JSONBContext jsonbContext = new JSONBContext();

    public HarvestOperation(InfomediaHarvesterConfig config,
                            BinaryFileStore binaryFileStore,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            RetrieverConnector retrieverConnector,
                            CreatorDetectorConnector creatorDetectorConnector) {
        this.config = config;
        this.binaryFileStore = binaryFileStore;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.retrieverConnector = retrieverConnector;
        this.creatorDetectorConnector = creatorDetectorConnector;
    }

    public int execute() throws HarvesterException {
        final StopWatch stopwatch = new StopWatch();
        int recordsHarvested = 0;
        try {
            final List<LocalDate> publicationDates = getPublicationDatesToHarvest(config);
            for (LocalDate publicationDate : publicationDates) {
                LOGGER.info("Harvesting publication date {}", publicationDate);

                try (JobBuilder jobBuilder = new JobBuilder(
                        binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                        JobSpecificationTemplate.create(config))) {

                    for (Article article : getArticles(publicationDate)) {
                        final AddiMetaData addiMetaData = createAddiMetaData(article);
                        LOGGER.info("harvested {}", addiMetaData.bibliographicRecordId());

                        final Record record = new Record();
                        record.setArticle(article);

                        try {
                            final List<CreatorNameSuggestion> creatorNameSuggestions =
                                    getCreatorNameSuggestions(article, addiMetaData.bibliographicRecordId());
                            if (!creatorNameSuggestions.isEmpty()) {
                                record.setCreatorNameSuggestions(creatorNameSuggestions);
                            }
                        } catch (RuntimeException | CreatorDetectorConnectorException e) {
                            final String errMsg = String.format("Getting creator name suggestions failed for article %s: %s",
                                    addiMetaData.bibliographicRecordId(), e.getMessage());
                            addiMetaData.withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, errMsg));
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

    private AddiMetaData createAddiMetaData(Article article) {
        final String docId = article.get("DOC_ID", String.class);
        return new AddiMetaData()
                .withTrackingId(String.format("Retriever.%s.%s", config.getLogId(), docId))
                .withBibliographicRecordId(docId)
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat(config.getContent().getFormat())
                .withCreationDate(getCreationDate(article, docId))
                .withLibraryRules(new AddiMetaData.LibraryRules());
    }

    private AddiRecord createAddiRecord(AddiMetaData metaData, Record record) throws HarvesterException {
        try {
            return new AddiRecord(
                    jsonbContext.marshall(metaData).getBytes(StandardCharsets.UTF_8),
                    jsonbContext.marshall(record).getBytes(StandardCharsets.UTF_8));
        } catch (JSONBException e) {
            throw new HarvesterException(e);
        }
    }

    /**
     * Retrieves all articles published on the specified date by performing paginated searches.
     * The method iterates through all pages of search results until all articles matching the
     * publication date and content source ID are retrieved. Each page contains a configurable
     * number of articles determined by the retriever page size.
     *
     * @param publicationDate the date for which to retrieve articles
     * @return a list containing all articles published on the specified date, or an empty list if no articles are found
     * @throws HarvesterException if an error occurs while communicating with the retriever connector or searching for articles
     */
    private List<Article> getArticles(LocalDate publicationDate) throws HarvesterException {
        try {
            final int pageSize = getRetrieverPageSize();

            int page = 1;
            final List<Article> allArticles = new ArrayList<>();
            while (true) {
                final ArticlesRequest request = ArticlesRequest.builder()
                        .fromDate(publicationDate)
                        .toDate(publicationDate)
                        .query("srcid:" + config.getContent().getId())
                        .page(page)
                        .size(pageSize)
                        .formatFulltextHtml(false)
                        .build();

                final ArticlesResponse response = retrieverConnector.searchArticles(request);

                final List<Article> articles = response.articles();
                if (articles.isEmpty()) {
                    break;
                }

                allArticles.addAll(articles);
                if (allArticles.size() >= response.total()) {
                    break;
                }

                page++;
            }

            return allArticles;
        } catch (RetrieverConnectorException e) {
            throw new HarvesterException("Unable to harvest articles", e);
        }
    }

    private Date getCreationDate(Article article, String bibliographicRecordId) throws HarvesterException {
        String publishingDate = "";
        try {
            publishingDate = article.get("PUBLISHING_DATE", String.class);
            if (publishingDate != null && !publishingDate.isBlank()) {
                return Date.from(safeParsePublishingDate(publishingDate));
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to get creation date from {} <{}>", bibliographicRecordId, publishingDate);
        }
        return null;
    }

    private List<CreatorNameSuggestion> getCreatorNameSuggestions(Article article, String bibliographicRecordId) throws CreatorDetectorConnectorException {
        if (article == null) {
            return Collections.emptyList();
        }
        final String byline = article.get("BYLINE", String.class);
        if (byline == null || byline.isBlank()) {
            return Collections.emptyList();
        }
        final DetectCreatorNamesRequest detectCreatorNamesRequest = new DetectCreatorNamesRequest(byline, bibliographicRecordId);
        final CreatorNameSuggestions creatorNameSuggestions = creatorDetectorConnector.detectCreatorNames(detectCreatorNamesRequest);
        if (creatorNameSuggestions.isEmpty()) {
            return Collections.emptyList();
        }
        return creatorNameSuggestions.getResults().stream()
                .filter(creatorNameSuggestion -> creatorNameSuggestion.authorityId() != null
                        && !creatorNameSuggestion.authorityId().isBlank())
                .toList();
    }

    private Instant safeParsePublishingDate(String publishingDate) {
        try {
            return Instant.parse(publishingDate); // works if 'Z' or offset is present
        } catch (DateTimeParseException e) {
            // Fallback: treat as UTC if no timezone info is present
            return LocalDateTime.parse(publishingDate, PUBLISHING_DATE_FALLBACK_PARSER)
                    .toInstant(ZoneOffset.UTC);
        }
    }
}
