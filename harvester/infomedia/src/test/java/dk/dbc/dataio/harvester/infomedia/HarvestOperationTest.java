/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.authornamesuggester.AuthorNameSuggesterConnector;
import dk.dbc.authornamesuggester.AuthorNameSuggesterConnectorException;
import dk.dbc.authornamesuggester.AuthorNameSuggestion;
import dk.dbc.authornamesuggester.AuthorNameSuggestions;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBeanTestUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
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
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.naming.Context;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private AuthorNameSuggesterConnector authorNameSuggesterConnector;
    private FlowStoreServiceConnector flowStoreServiceConnector;
    private InfomediaConnector infomediaConnector;
    private JobStoreServiceConnector jobStoreServiceConnector;
    private MockedFileStoreServiceConnector fileStoreServiceConnector;
    private Path harvesterTmpFile;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setInitialContext() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setupMocks() throws IOException, JobStoreServiceConnectorException {

        // Enable JNDI lookup of base path for BinaryFileStoreBean
        final File testFolder = tmpFolder.newFolder();
        InMemoryInitialContextFactory.bind("bfs/home", testFolder.toString());

        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterTmpFile = tmpFolder.newFile().toPath();
        fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        fileStoreServiceConnector.destinations.add(harvesterTmpFile);

        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                .thenReturn(new JobInfoSnapshot());

        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        infomediaConnector = mock(InfomediaConnector.class);
        authorNameSuggesterConnector = mock(AuthorNameSuggesterConnector.class);
    }

    @Test
    public void execute() throws HarvesterException, InfomediaConnectorException,
                                 FlowStoreServiceConnectorException, AuthorNameSuggesterConnectorException, JobStoreServiceConnectorException {
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
        final Instant yesterday = today.minus(1, ChronoUnit.DAYS);
        when(infomediaConnector.searchArticleIds(today, today, today, config.getContent().getId()))
                .thenReturn(articleIds);
        when(infomediaConnector.searchArticleIds(yesterday, yesterday, yesterday, config.getContent().getId()))
                .thenReturn(Collections.emptySet());
        when(infomediaConnector.getArticles(articleIds))
                .thenReturn(articleList);
        when(infomediaConnector.getArticles(Collections.emptySet()))
                .thenReturn(emptyArticleList);

        // Setting next publication date to yesterday tests that
        // multiple searchArticleIds calls are being made.
        config.getContent().withNextPublicationDate(Date.from(yesterday));

        final AuthorNameSuggestions authorOneASuggestions = new AuthorNameSuggestions();
        authorOneASuggestions.setAutNames(Collections.singletonList(new AuthorNameSuggestion.Builder().withInputName("authorOneA_AutId").withAuthority("AutIdA").build()));
        authorOneASuggestions.setNerNames(Collections.singletonList(new AuthorNameSuggestion.Builder().withInputName("authorOneA_AutId").withAuthority("AutIdA").build()));
        when(authorNameSuggesterConnector
                .getSuggestions(Collections.singletonList(articleOne.getAuthors().get(0))))
                .thenReturn(authorOneASuggestions);
        final AuthorNameSuggestions authorOneBSuggestions = new AuthorNameSuggestions();
        authorOneBSuggestions.setAutNames(Collections.singletonList(new AuthorNameSuggestion.Builder().withInputName("authorOneB_AutId").withAuthority("AutIdB").build()));
        authorOneBSuggestions.setNerNames(Collections.singletonList(new AuthorNameSuggestion.Builder().withInputName("authorOneB_AutId").withAuthority("AutIdB").build()));
        when(authorNameSuggesterConnector
                .getSuggestions(Collections.singletonList(articleOne.getAuthors().get(1))))
                .thenReturn(authorOneBSuggestions);

        when(authorNameSuggesterConnector
                .getSuggestions(Collections.singletonList(articleTwo.getAuthors().get(0))))
                .thenThrow(new AuthorNameSuggesterConnectorException("died"));

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
                "Getting author name suggestions failed for %s: died",
                articleTwo.getAuthors().get(0)))));
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
                                    "<input-name>"+ authorOneASuggestions.getAutNames().get(0).getInputName() + "</input-name>" +
                                    "<authority>"+ authorOneASuggestions.getAutNames().get(0).getAuthority() + "</authority>" +
                                "</aut-name>" +
                            "</aut-names>" +
                            "<ner-names>" +
                                "<ner-name>" +
                                     "<input-name>"+ authorOneASuggestions.getAutNames().get(0).getInputName() + "</input-name>" +
                                     "<authority>"+ authorOneASuggestions.getAutNames().get(0).getAuthority() + "</authority>" +
                                "</ner-name>" +
                            "</ner-names>" +
                        "</author-name-suggestion>" +
                        "<author-name-suggestion>" +
                            "<aut-names>" +
                                "<aut-name>" +
                                    "<input-name>"+ authorOneBSuggestions.getAutNames().get(0).getInputName() + "</input-name>" +
                                    "<authority>"+ authorOneBSuggestions.getAutNames().get(0).getAuthority() + "</authority>" +
                                "</aut-name>" +
                            "</aut-names>" +
                            "<ner-names>" +
                                "<ner-name>" +
                                    "<input-name>"+ authorOneBSuggestions.getAutNames().get(0).getInputName() + "</input-name>" +
                                    "<authority>"+ authorOneBSuggestions.getAutNames().get(0).getAuthority() + "</authority>" +
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
        return new HarvestOperation(config,
                BinaryFileStoreBeanTestUtil
                        .getBinaryFileStoreBean("bfs/home"),
                flowStoreServiceConnector,
                fileStoreServiceConnector,
                jobStoreServiceConnector,
                infomediaConnector,
                authorNameSuggesterConnector);
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