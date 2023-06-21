package dk.dbc.dataio.harvester.periodicjobs;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.macroexpansion.MacroSubstitutor;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.testee.NonContainerManagedExecutorService;
import dk.dbc.testee.SameThreadExecutorService;
import dk.dbc.weekresolver.WeekResolverConnectorException;
import dk.dbc.weekresolver.WeekResolverConnectorFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.concurrent.ManagedExecutorService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class MacroSubstitutorTest {
    /*
    private static final Logger LOGGER = LoggerFactory.getLogger(MacroSubstitutorTest.class);

    private WireMockServer wireMockServer;

    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    private final ManagedExecutorService managedExecutorService = new NonContainerManagedExecutorService(
            new SameThreadExecutorService());
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final BinaryFileStore binaryFileStore = mock(BinaryFileStore.class);

    private HarvestOperation harvestOperation;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void setup() throws WeekResolverConnectorException {
        environmentVariables.set("TZ", "Europe/Copenhagen");
        wireMockServer = startWireMockServer();
        harvestOperation = newHarvestOperation();
    }

    @After
    public void cleanup() {
        wireMockServer.stop();
    }

    @Test
    public void testNextweekPattern() {
        // Note: This pattern does not use the week-resolver, it only calculates weeks from the given date

        final String inputQuery = "term.kk:${__NEXTWEEK_BKM__} OR term.kk:${__NEXTWEEK_ACC__} OR term.kk:${__NEXTWEEK_DPF__}";
        final String expectedQuery = "term.kk:BKM202325 OR term.kk:ACC202325 OR term.kk:DPF202325";

        ZonedDateTime now = Instant.parse("2023-06-16T12:00:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), harvestOperation::catalogueCodeToWeekCode);
        assertThat(macroSubstitutor.replace(inputQuery), is(expectedQuery));
    }

    @Test
    public void testWeekcodePatternBeforeShiftday() {
        final String inputQuery = "term.kk:${__WEEKCODE_BKM__} OR term.kk:${__WEEKCODE_ACC__} OR term.kk:${__WEEKCODE_DPF__}";
        final String expectedQuery = "term.kk:BKM202324 OR term.kk:ACC202324 OR term.kk:DPF202324";

        ZonedDateTime now = Instant.parse("2023-06-15T12:00:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), harvestOperation::catalogueCodeToWeekCode);
        assertThat(macroSubstitutor.replace(inputQuery), is(expectedQuery));
    }

    @Test
    public void testWeekcodePatternOnShiftday() {
        final String inputQuery = "term.kk:${__WEEKCODE_BKM__} OR term.kk:${__WEEKCODE_ACC__} OR term.kk:${__WEEKCODE_DPF__}";
        final String expectedQuery = "term.kk:BKM202325 OR term.kk:ACC202324 OR term.kk:DPF202325";

        ZonedDateTime now = Instant.parse("2023-06-16T12:00:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), harvestOperation::catalogueCodeToWeekCode);
        assertThat(macroSubstitutor.replace(inputQuery), is(expectedQuery));
    }

    @Test
    public void testWeekcodePatternMinusBeforeShiftday() {
        final String inputQuery = "term.kk:${__WEEKCODE_BKM_MINUS_3__} OR term.kk:${__WEEKCODE_ACC_MINUS_3__} OR term.kk:${__WEEKCODE_DPF_MINUS_3__}";
        final String expectedQuery = "term.kk:BKM202321 OR term.kk:ACC202321 OR term.kk:DPF202321";

        ZonedDateTime now = Instant.parse("2023-06-15T12:00:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), harvestOperation::catalogueCodeToWeekCode);
        assertThat(macroSubstitutor.replace(inputQuery), is(expectedQuery));
    }

    @Test
    public void testWeekcodePatternMinusOnShiftday() {
        final String inputQuery = "term.kk:${__WEEKCODE_BKM_MINUS_3__} OR term.kk:${__WEEKCODE_ACC_MINUS_3__} OR term.kk:${__WEEKCODE_DPF_MINUS_3__}";
        final String expectedQuery = "term.kk:BKM202322 OR term.kk:ACC202321 OR term.kk:DPF202322";

        ZonedDateTime now = Instant.parse("2023-06-16T12:00:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), harvestOperation::catalogueCodeToWeekCode);
        assertThat(macroSubstitutor.replace(inputQuery), is(expectedQuery));
    }

    @Test
    public void testWeekcodePatternMinus() {
        // Note: This was the actual query that was reported as faulty and resulted in the modification to use
        //       the (correct) /current endpoint on the weekresolver. MS-4359.

        final String inputQuery = "term.kk:${__WEEKCODE_DBF_MINUS_3__} OR term.kk:${__WEEKCODE_GBF_MINUS_3__} OR term.kk:${__WEEKCODE_DLF_MINUS_3__}";
        final String expectedQuery = "term.kk:DBF202316 OR term.kk:GBF202316 OR term.kk:DLF202316";

        ZonedDateTime now = Instant.parse("2023-05-10T14:34:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), harvestOperation::catalogueCodeToWeekCode);
        assertThat(macroSubstitutor.replace(inputQuery), is(expectedQuery));

        // The replacement is tested twice due to the initial report that stated that the required diff changed
        // from -5 to -6 around 15 o'clock on this day. This has not been possible to reproduce and is assumed to be
        // a matter of slight confusion on the user end due to the overall erroneous results.

        now = Instant.parse("2023-05-10T15:29:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        macroSubstitutor = new MacroSubstitutor(now.toInstant(), harvestOperation::catalogueCodeToWeekCode);
        assertThat(macroSubstitutor.replace(inputQuery), is(expectedQuery));
    }

    @Test
    public void testWeekcodePatternPlusBeforeShiftday() {
        final String inputQuery = "term.kk:${__WEEKCODE_BKM_PLUS_3__} OR term.kk:${__WEEKCODE_ACC_PLUS_3__} OR term.kk:${__WEEKCODE_DPF_PLUS_3__}";
        final String expectedQuery = "term.kk:BKM202327 OR term.kk:ACC202327 OR term.kk:DPF202327";

        ZonedDateTime now = Instant.parse("2023-06-15T12:00:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), harvestOperation::catalogueCodeToWeekCode);
        assertThat(macroSubstitutor.replace(inputQuery), is(expectedQuery));
    }

    @Test
    public void testWeekcodePatternPlusOnShiftday() {
        final String inputQuery = "term.kk:${__WEEKCODE_BKM_PLUS_3__} OR term.kk:${__WEEKCODE_ACC_PLUS_3__} OR term.kk:${__WEEKCODE_DPF_PLUS_3__}";
        final String expectedQuery = "term.kk:BKM202328 OR term.kk:ACC202327 OR term.kk:DPF202328";

        ZonedDateTime now = Instant.parse("2023-06-16T12:00:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), harvestOperation::catalogueCodeToWeekCode);
        assertThat(macroSubstitutor.replace(inputQuery), is(expectedQuery));
    }

    @Test
    public void testWeekPattern() {
        // Note: This pattern does not use the week-resolver, it only calculates weeks from the given date

        final String inputQuery = "term.kk:${__WEEK_PLUS_3__} OR term.kk:${__WEEK_PLUS_0__} OR term.kk:${__WEEK_MINUS_0__} OR term.kk:${__WEEK_MINUS_3__}";
        final String expectedQuery = "term.kk:202327 OR term.kk:202324 OR term.kk:202324 OR term.kk:202321";

        ZonedDateTime now = Instant.parse("2023-06-16T12:00:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), harvestOperation::catalogueCodeToWeekCode);
        assertThat(macroSubstitutor.replace(inputQuery), is(expectedQuery));
    }

    private WireMockServer startWireMockServer() {
        WireMockServer server = new WireMockServer(new WireMockConfiguration().dynamicPort());
        server.start();
        configureFor("localhost", server.port());
        LOGGER.info("Wiremock server at port:{}", server.port());
        return server;
    }

    private HarvestOperation newHarvestOperation() throws WeekResolverConnectorException {
        PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withSubmitterNumber("123456"));
        return spy(new HarvestOperation(config,
                binaryFileStore,
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                WeekResolverConnectorFactory.create(wireMockServer.baseUrl()),
                managedExecutorService,
                rawRepoConnector));
    }
     */
}
