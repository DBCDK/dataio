package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
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
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.AddiFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.Expectation;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private CreatorDetectorConnector creatorDetectorConnector;
    private FlowStoreServiceConnector flowStoreServiceConnector;
    private RetrieverConnector retrieverConnector;
    private JobStoreServiceConnector jobStoreServiceConnector;
    private MockedFileStoreServiceConnector fileStoreServiceConnector;
    private Path harvesterTmpFile;

    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void setupMocks() throws IOException, JobStoreServiceConnectorException {
        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterTmpFile = Files.createFile(tmpFolder.resolve("im-test_" + UUID.randomUUID() + ".tmp"));
        fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        fileStoreServiceConnector.destinations.add(harvesterTmpFile);

        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).thenReturn(new JobInfoSnapshot());

        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        retrieverConnector = mock(RetrieverConnector.class);
        creatorDetectorConnector = mock(CreatorDetectorConnector.class);
    }

    @BeforeEach
    void setUpTimeZone() {
        HarvestOperation.setTimezoneSupplierForTests(() -> ZoneId.of("Europe/Copenhagen"));
    }

    @AfterEach
    void resetTimeZone() {
        HarvestOperation.setTimezoneSupplierForTests(null);
    }

    @BeforeEach
    void setUpRetrieverPageSize() {
        HarvestOperation.setRetrieverPageSizeSupplierForTests(() -> 2);
    }

    @AfterEach
    void resetRetrieverPageSize() {
        HarvestOperation.setRetrieverPageSizeSupplierForTests(null);   
    }

    @Test
    public void execute() throws HarvesterException, RetrieverConnectorException, FlowStoreServiceConnectorException, CreatorDetectorConnectorException, JobStoreServiceConnectorException {
        Article articleOne = new Article();
        articleOne.set("DOC_ID", "one");
        articleOne.set("PUBLISHING_DATE", "2026-03-21T02:00:00");
        articleOne.set("BYLINE", "kim skotte kiri kim lassen");

        Article articleTwo = new Article();
        articleTwo.set("DOC_ID", "two");
        articleTwo.set("PUBLISHING_DATE", "2026-03-22T02:00:00");
        articleTwo.set("BYLINE", "authorTwo");

        Article articleThree = new Article();
        articleThree.set("DOC_ID", "three");
        articleThree.set("PUBLISHING_DATE", "2026-03-23T02:00:00");

        InfomediaHarvesterConfig config = newConfig();

        LocalDate today = LocalDate.now(HarvestOperation.getTimezone());
        LocalDate yesterday = today.minusDays(1);

        // Also tests paging of retriever responses.
        ArticlesRequest todayRequest1 = ArticlesRequest.builder()
                .fromDate(today)
                .toDate(today)
                .query("srcid:" + config.getContent().getId())
                .page(1)
                .size(2)
                .formatFulltextHtml(false)
                .build();
        ArticlesRequest todayRequest2 = ArticlesRequest.builder()
                .fromDate(today)
                .toDate(today)
                .query("srcid:" + config.getContent().getId())
                .page(2)
                .size(2)
                .formatFulltextHtml(false)
                .build();
        ArticlesRequest yesterdayRequest1 = ArticlesRequest.builder()
                .fromDate(yesterday)
                .toDate(yesterday)
                .query("srcid:" + config.getContent().getId())
                .page(1)
                .size(2)
                .formatFulltextHtml(false)
                .build();

        when(retrieverConnector.searchArticles(todayRequest1))
                .thenReturn(new ArticlesResponse(3, List.of(articleOne, articleTwo)));
        when(retrieverConnector.searchArticles(todayRequest2))
                .thenReturn(new ArticlesResponse(3, List.of(articleThree)));
        when(retrieverConnector.searchArticles(yesterdayRequest1))
                .thenReturn(new ArticlesResponse(0, Collections.emptyList()));

        // Setting next publication date to yesterday tests that
        // multiple searchArticles calls are being made.
        config.getContent().withNextPublicationDate(Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        DetectCreatorNamesRequest articleOneDetectCreatorNamesRequest = new DetectCreatorNamesRequest("kim skotte kiri kim lassen", "one");
        CreatorNameSuggestions articleOneCreatorNameSuggestions = new CreatorNameSuggestions();
        articleOneCreatorNameSuggestions.setResults(List.of(
                new CreatorNameSuggestion("kim skotte", "870979:68943574", "kim skotte", 0.873639702796936, 7.805474625270857),
                new CreatorNameSuggestion("kiri kim lassen", "870979:19253007", "kiri kim lassen", 0.873639702796936, 5.407171771460119)
        ));
        when(creatorDetectorConnector.detectCreatorNames(eq(articleOneDetectCreatorNamesRequest)))
                .thenReturn(articleOneCreatorNameSuggestions);

        DetectCreatorNamesRequest articleTwoDetectCreatorNamesRequest = new DetectCreatorNamesRequest("authorTwo", "two");
        when(creatorDetectorConnector.detectCreatorNames(eq(articleTwoDetectCreatorNamesRequest)))
                .thenThrow(new CreatorDetectorConnectorException("died"));

        List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("test-format").withBibliographicRecordId("one")
                .withTrackingId("Retriever.test.one").withDeleted(false));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("test-format")
                .withBibliographicRecordId("two")
                .withTrackingId("Retriever.test.two")
                .withDeleted(false)
                .withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, String.format("Getting creator name suggestions failed for article %s: died", "two"))));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("test-format")
                .withBibliographicRecordId("three")
                .withTrackingId("Retriever.test.three")
                .withDeleted(false));

        List<Expectation> addiContentExpectations = new ArrayList<>();
        addiContentExpectations.add(new Expectation(
                "{\"article\":{\"DOC_ID\":\"one\",\"PUBLISHING_DATE\":\"2026-03-21T02:00:00\",\"BYLINE\":\"kim skotte kiri kim lassen\"},\"creatorNameSuggestions\":[{\"detected_ner_name\":\"kim skotte\",\"authority_id\":\"870979:68943574\",\"authority_name_normalized\":\"kim skotte\",\"match_score\":0.873639702796936,\"rerank_score\":7.805474625270857},{\"detected_ner_name\":\"kiri kim lassen\",\"authority_id\":\"870979:19253007\",\"authority_name_normalized\":\"kiri kim lassen\",\"match_score\":0.873639702796936,\"rerank_score\":5.407171771460119}]}"));
        addiContentExpectations.add(new Expectation(
                "{\"article\":{\"DOC_ID\":\"two\",\"PUBLISHING_DATE\":\"2026-03-22T02:00:00\",\"BYLINE\":\"authorTwo\"}}"));
        addiContentExpectations.add(new Expectation(
                "{\"article\":{\"DOC_ID\":\"three\",\"PUBLISHING_DATE\":\"2026-03-23T02:00:00\"}}"));

        Instant expectedNextPublicationDate = Instant.now()
                .plus(1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS);

        createHarvestOperation(config).execute();

        AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(InfomediaHarvesterConfig.class));

        assertThat(config.getContent().getNextPublicationDate(), is(Date.from(expectedNextPublicationDate)));
    }

    @Test
    public void noCreatorNameSuggestionsForMissingByline() throws HarvesterException, RetrieverConnectorException, FlowStoreServiceConnectorException, JobStoreServiceConnectorException {
        Article articleNoAuthors = new Article();
        articleNoAuthors.set("DOC_ID", "no-authors");
        articleNoAuthors.set("PUBLISHING_DATE", "2026-03-23T02:00:00");

        InfomediaHarvesterConfig config = newConfig();

        LocalDate today = LocalDate.now(HarvestOperation.getTimezone());

        ArticlesRequest todayRequest = ArticlesRequest.builder()
                .fromDate(today)
                .toDate(today)
                .query("srcid:" + config.getContent().getId())
                .page(1)
                .size(2)
                .formatFulltextHtml(false)
                .build();

        when(retrieverConnector.searchArticles(todayRequest))
                .thenReturn(new ArticlesResponse(1, List.of(articleNoAuthors)));

        config.getContent().withNextPublicationDate(Date.from(
                today.atStartOfDay(HarvestOperation.getTimezone())
                        .toInstant()));

        List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("test-format")
                .withBibliographicRecordId("no-authors")
                .withTrackingId("Retriever.test.no-authors")
                .withDeleted(false));

        List<Expectation> addiContentExpectations = new ArrayList<>();
        addiContentExpectations.add(new Expectation(
                "{\"article\":{\"DOC_ID\":\"no-authors\",\"PUBLISHING_DATE\":\"2026-03-23T02:00:00\"}}"));

        Instant expectedNextPublicationDate = Instant.now()
                .plus(1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS);

        createHarvestOperation(config).execute();

        AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(InfomediaHarvesterConfig.class));

        assertThat(config.getContent().getNextPublicationDate(), is(Date.from(expectedNextPublicationDate)));
    }

    @Test
    public void getPublicationDatesToHarvestWhenNextPublicationDateIsNull() {
        LocalDate today = LocalDate.now(HarvestOperation.getTimezone());
        List<LocalDate> dates = HarvestOperation.getPublicationDatesToHarvest(newConfig());
        assertThat(dates, is(List.of(today)));
    }

    @Test
    public void getPublicationDatesToHarvestWhenNextPublicationDateIsInTheFuture() {
        LocalDate today = LocalDate.now(HarvestOperation.getTimezone());
        InfomediaHarvesterConfig config = newConfig();
        config.getContent().withNextPublicationDate(Date.from(
                today.plusDays(1).atStartOfDay(HarvestOperation.getTimezone())
                        .toInstant()));
        List<LocalDate> dates = HarvestOperation.getPublicationDatesToHarvest(config);
        assertThat(dates, is(List.of(today)));
    }

    @Test
    public void getPublicationDatesToHarvestWhenNextPublicationDateIsInThePast() {
        LocalDate today = LocalDate.now(HarvestOperation.getTimezone());
        InfomediaHarvesterConfig config = newConfig();
        config.getContent().withNextPublicationDate(Date.from(
                today.minusDays(2).atStartOfDay(HarvestOperation.getTimezone())
                        .toInstant()));
        List<LocalDate> dates = HarvestOperation.getPublicationDatesToHarvest(config);
        assertThat(dates, is(List.of(today.minusDays(2), today.minusDays(1), today)));
    }

    private HarvestOperation createHarvestOperation(InfomediaHarvesterConfig config) {
        try {
            return new HarvestOperation(config, new BinaryFileStoreFsImpl(Files.createDirectory(tmpFolder.resolve("im-op-test-" + UUID.randomUUID()))), flowStoreServiceConnector, fileStoreServiceConnector, jobStoreServiceConnector, retrieverConnector, creatorDetectorConnector);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private InfomediaHarvesterConfig newConfig() {
        InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(1, 2, new InfomediaHarvesterConfig.Content());
        config.getContent().withId("test").withFormat("test-format").withDestination("test-destination");
        return config;
    }
}
