package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.autonomen.AutoNomenConnector;
import dk.dbc.autonomen.AutoNomenConnectorException;
import dk.dbc.autonomen.AutoNomenSuggestion;
import dk.dbc.autonomen.AutoNomenSuggestions;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
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
import dk.dbc.infomedia.Article;
import dk.dbc.infomedia.ArticleList;
import dk.dbc.infomedia.InfomediaConnector;
import dk.dbc.infomedia.InfomediaConnectorException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private AutoNomenConnector autoNomenConnector;
    private FlowStoreServiceConnector flowStoreServiceConnector;
    private InfomediaConnector infomediaConnector;
    private JobStoreServiceConnector jobStoreServiceConnector;
    private MockedFileStoreServiceConnector fileStoreServiceConnector;
    private Path harvesterTmpFile;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setupMocks() throws IOException, JobStoreServiceConnectorException {
        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterTmpFile = tmpFolder.newFile().toPath();
        fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        fileStoreServiceConnector.destinations.add(harvesterTmpFile);

        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                .thenReturn(new JobInfoSnapshot());

        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        infomediaConnector = mock(InfomediaConnector.class);
        autoNomenConnector = mock(AutoNomenConnector.class);
    }

    @Test
    public void execute() throws HarvesterException, InfomediaConnectorException,
            FlowStoreServiceConnectorException, AutoNomenConnectorException, JobStoreServiceConnectorException {
        final Set<String> articleIds = new HashSet<>(Arrays.asList("one", "two", "three"));
        final List<Article> articles = new ArrayList<>(articleIds.size());
        final Article articleOne = new Article();
        articleOne.setArticleId("one");
        articleOne.setPublishDate(Instant.now().toString());
        articleOne.setAuthors(Arrays.asList("authorOneA", "authorOneB"));
        articles.add(articleOne);
        final Article articleTwo = new Article();
        articleTwo.setArticleId("two");
        articleTwo.setPublishDate(Instant.now().toString());
        articleTwo.setAuthors(Collections.singletonList("authorTwo"));
        articles.add(articleTwo);
        final Article articleThree = new Article();
        articleThree.setArticleId("three");
        articleThree.setPublishDate(Instant.now().toString());
        articles.add(articleThree);
        final ArticleList articleList = new ArticleList();
        articleList.setArticles(articles);
        final ArticleList emptyArticleList = new ArticleList();
        emptyArticleList.setArticles(Collections.emptyList());

        final InfomediaHarvesterConfig config = newConfig();

        // There is a small risk that this test will
        // fail if run very close to midnight so that
        // the 'today' used to match the mocked call
        // differs from the 'today' used by the execute()
        // method.

        final Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        final Duration oneDay = Duration.ofHours(23).plusMinutes(59).plusSeconds(59);
        final Instant yesterday = today.minus(1, ChronoUnit.DAYS);
        when(infomediaConnector.searchArticleIdsByPublishDate(today, oneDay, config.getContent().getId()))
                .thenReturn(articleIds);
        when(infomediaConnector.searchArticleIdsByPublishDate(yesterday, oneDay, config.getContent().getId()))
                .thenReturn(Collections.emptySet());
        when(infomediaConnector.getArticles(articleIds))
                .thenReturn(articleList);
        when(infomediaConnector.getArticles(Collections.emptySet()))
                .thenReturn(emptyArticleList);

        // Setting next publication date to yesterday tests that
        // multiple searchArticleIds calls are being made.
        config.getContent().withNextPublicationDate(Date.from(yesterday));

        final AutoNomenSuggestions articleOneSuggestions = new AutoNomenSuggestions();
        articleOneSuggestions.setAutNames(Arrays.asList(new AutoNomenSuggestion.Builder().withInputName("authorOneA_AutId").withAuthority("AutIdA").build(),
                new AutoNomenSuggestion.Builder().withInputName("authorOneB_AutId").withAuthority("AutIdB").build()));
        articleOneSuggestions.setNerNames(Arrays.asList(new AutoNomenSuggestion.Builder().withInputName("authorOneA_AutId").withAuthority("AutIdA").build(),
                new AutoNomenSuggestion.Builder().withInputName("authorOneB_AutId").withAuthority("AutIdB").build()));
        when(autoNomenConnector
                .getSuggestions(articleOne.getArticleId()))
                .thenReturn(articleOneSuggestions);

        when(autoNomenConnector
                .getSuggestions(articleTwo.getArticleId()))
                .thenThrow(new AutoNomenConnectorException("died"));

        final AutoNomenSuggestions emptySuggestions = new AutoNomenSuggestions();
        emptySuggestions.setAutNames(new ArrayList<>());
        emptySuggestions.setNerNames(new ArrayList<>());

        when(autoNomenConnector
                .getSuggestions(articleThree.getArticleId()))
                .thenReturn(emptySuggestions);

        final List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("test-format")
                .withBibliographicRecordId("one")
                .withTrackingId("Infomedia.test.one")
                .withDeleted(false));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("test-format")
                .withBibliographicRecordId("two")
                .withTrackingId("Infomedia.test.two")
                .withDeleted(false)
                .withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, String.format(
                "Getting author name suggestions failed for article %s: died",
                articleTwo.getArticleId()))));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("test-format")
                .withBibliographicRecordId("three")
                .withTrackingId("Infomedia.test.three")
                .withDeleted(false));

        final List<Expectation> addiContentExpectations = new ArrayList<>();
        addiContentExpectations.add(new Expectation(
                "<record>" +
                    "<infomedia>" +
                        "<article>" +
                            "<Heading/>" +
                            "<SubHeading/>" +
                            "<BodyText/>" +
                            "<PublishDate>" + articleOne.getPublishDate() + "</PublishDate>" +
                            "<Authors>" +
                                "<Author>" + articleOne.getAuthors().get(0) + "</Author>" +
                                "<Author>" + articleOne.getAuthors().get(1) + "</Author>" +
                            "</Authors>" +
                            "<ArticleUrl/>" +
                            "<Paragraph/>" +
                            "<Source/>" +
                            "<WordCount/>" +
                            "<ArticleId>one</ArticleId>" +
                            "<Section/>" +
                            "<Lead/>" +
                        "</article>" +
                    "</infomedia>" +
                    "<author-name-suggestions>" +
                       "<author-name-suggestion>" +
                            "<aut-names>" +
                                "<aut-name>" +
                                    "<input-name>" + articleOneSuggestions.getAutNames().get(0).getInputName() + "</input-name>" +
                                    "<authority>" + articleOneSuggestions.getAutNames().get(0).getAuthority() + "</authority>" +
                                "</aut-name>" +
                                "<aut-name>" +
                                    "<input-name>" + articleOneSuggestions.getAutNames().get(1).getInputName() + "</input-name>" +
                                    "<authority>" + articleOneSuggestions.getAutNames().get(1).getAuthority() + "</authority>" +
                                "</aut-name>" +
                            "</aut-names>" +
                            "<ner-names>" +
                                "<ner-name>" +
                                    "<input-name>" + articleOneSuggestions.getAutNames().get(0).getInputName() + "</input-name>" +
                                    "<authority>" + articleOneSuggestions.getAutNames().get(0).getAuthority() + "</authority>" +
                                "</ner-name>" +
                                "<ner-name>" +
                                    "<input-name>" + articleOneSuggestions.getAutNames().get(1).getInputName() + "</input-name>" +
                                    "<authority>" + articleOneSuggestions.getAutNames().get(1).getAuthority() + "</authority>" +
                                "</ner-name>" +
                            "</ner-names>" +
                        "</author-name-suggestion>" +
                    "</author-name-suggestions>" +
                "</record>"));
        addiContentExpectations.add(new Expectation(
                "<record>" +
                    "<infomedia>" +
                        "<article>" +
                            "<Heading/>" +
                            "<SubHeading/>" +
                            "<BodyText/>" +
                            "<PublishDate>" + articleTwo.getPublishDate() + "</PublishDate>" +
                            "<ArticleUrl/>" +
                            "<Paragraph/>" +
                            "<Source/>" +
                            "<WordCount/>" +
                            "<ArticleId>two</ArticleId>" +
                            "<Section/>" +
                            "<Lead/>" +
                        "</article>" +
                    "</infomedia>" +
                "</record>"));
        addiContentExpectations.add(new Expectation(
                "<record>" +
                    "<infomedia>" +
                        "<article>" +
                            "<Heading/>" +
                            "<SubHeading/>" +
                            "<BodyText/>" +
                            "<PublishDate>" + articleThree.getPublishDate() + "</PublishDate>" +
                            "<ArticleUrl/>" +
                            "<Paragraph/>" +
                            "<Source/>" +
                            "<WordCount/>" +
                            "<ArticleId>three</ArticleId>" +
                            "<Section/>" +
                            "<Lead/>" +
                        "</article>" +
                    "</infomedia>" +
                "</record>"));

        createHarvestOperation(config).execute();

        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(InfomediaHarvesterConfig.class));

        assertThat(config.getContent().getNextPublicationDate(),
                is(Date.from(Instant.now()
                        .plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS))));
    }

    @Test
    public void noAuthorNameSuggestionsForEmptyAuthors() throws HarvesterException, InfomediaConnectorException,
            FlowStoreServiceConnectorException, JobStoreServiceConnectorException, AutoNomenConnectorException {
        final Set<String> articleIds = new HashSet<>(Collections.singletonList("no-authors"));
        final List<Article> articles = new ArrayList<>(articleIds.size());
        final Article articleNoAuthors = new Article();
        articleNoAuthors.setArticleId("no-authors");
        articleNoAuthors.setPublishDate(Instant.now().toString());
        articleNoAuthors.setAuthors(Arrays.asList("", ""));
        articles.add(articleNoAuthors);
        final ArticleList articleList = new ArticleList();
        articleList.setArticles(articles);
        final ArticleList emptyArticleList = new ArticleList();
        emptyArticleList.setArticles(Collections.emptyList());

        final InfomediaHarvesterConfig config = newConfig();

        // There is a small risk that this test will
        // fail if run very close to midnight so that
        // the 'today' used to match the mocked call
        // differs from the 'today' used by the execute()
        // method.

        final Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        final Instant yesterday = today.minus(1, ChronoUnit.DAYS);
        final Duration oneDay = Duration.ofHours(23).plusMinutes(59).plusSeconds(59);
        when(infomediaConnector.searchArticleIdsByPublishDate(today, oneDay, config.getContent().getId()))
                .thenReturn(articleIds);
        when(infomediaConnector.searchArticleIdsByPublishDate(yesterday, oneDay, config.getContent().getId()))
                .thenReturn(Collections.emptySet());
        when(infomediaConnector.getArticles(articleIds))
                .thenReturn(articleList);
        when(infomediaConnector.getArticles(Collections.emptySet()))
                .thenReturn(emptyArticleList);

        final AutoNomenSuggestions emptySuggestions = new AutoNomenSuggestions();
        emptySuggestions.setAutNames(new ArrayList<>());
        emptySuggestions.setNerNames(new ArrayList<>());

        when(autoNomenConnector
                .getSuggestions(articleNoAuthors.getArticleId()))
                .thenReturn(emptySuggestions);

        // Setting next publication date to yesterday tests that
        // multiple searchArticleIds calls are being made.
        config.getContent().withNextPublicationDate(Date.from(yesterday));

        final List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("test-format")
                .withBibliographicRecordId("no-authors")
                .withTrackingId("Infomedia.test.no-authors")
                .withDeleted(false));

        final List<Expectation> addiContentExpectations = new ArrayList<>();
        addiContentExpectations.add(new Expectation(
                "<record>" +
                        "<infomedia>" +
                        "<article>" +
                        "<Heading/>" +
                        "<SubHeading/>" +
                        "<BodyText/>" +
                        "<PublishDate>" + articleNoAuthors.getPublishDate() + "</PublishDate>" +
                        "<Authors>" +
                        "<Author></Author>" +
                        "<Author></Author>" +
                        "</Authors>" +
                        "<ArticleUrl/>" +
                        "<Paragraph/>" +
                        "<Source/>" +
                        "<WordCount/>" +
                        "<ArticleId>no-authors</ArticleId>" +
                        "<Section/>" +
                        "<Lead/>" +
                        "</article>" +
                        "</infomedia>" +
                        "</record>"));

        createHarvestOperation(config).execute();

        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(InfomediaHarvesterConfig.class));

        assertThat(config.getContent().getNextPublicationDate(),
                is(Date.from(Instant.now()
                        .plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS))));
    }

    @Test
    public void getPublicationDatesToHarvestWhenNextPublicationDateIsNull() {
        final List<Instant> dates = HarvestOperation.getPublicationDatesToHarvest(newConfig());
        assertThat(dates, is(Collections.singletonList(
                Instant.now().truncatedTo(ChronoUnit.DAYS))));
    }

    @Test
    public void getPublicationDatesToHarvestWhenNextPublicationDateIsInTheFuture() {
        final InfomediaHarvesterConfig config = newConfig();
        config.getContent().withNextPublicationDate(
                Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
        final List<Instant> dates = HarvestOperation.getPublicationDatesToHarvest(config);
        assertThat(dates, is(Collections.singletonList(
                Instant.now().truncatedTo(ChronoUnit.DAYS))));
    }

    @Test
    public void getPublicationDatesToHarvestWhenNextPublicationDateIsInThePast() {
        final InfomediaHarvesterConfig config = newConfig();
        config.getContent().withNextPublicationDate(
                Date.from(Instant.now().minus(2, ChronoUnit.DAYS)));
        final List<Instant> dates = HarvestOperation.getPublicationDatesToHarvest(config);
        assertThat(dates, is(Arrays.asList(
                Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                Instant.now().truncatedTo(ChronoUnit.DAYS))));
    }

    private HarvestOperation createHarvestOperation(InfomediaHarvesterConfig config) {
        try {
            return new HarvestOperation(config,
                    new BinaryFileStoreFsImpl(tmpFolder.newFolder().toPath()),
                    flowStoreServiceConnector,
                    fileStoreServiceConnector,
                    jobStoreServiceConnector,
                    infomediaConnector,
                    autoNomenConnector);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private InfomediaHarvesterConfig newConfig() {
        final InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(
                1, 2, new InfomediaHarvesterConfig.Content());
        config.getContent()
                .withId("test")
                .withFormat("test-format")
                .withDestination("test-destination");
        return config;
    }
}
