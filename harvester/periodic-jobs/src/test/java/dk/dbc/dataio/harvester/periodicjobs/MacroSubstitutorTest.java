package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.macroexpansion.MacroSubstitutor;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.testee.NonContainerManagedExecutorService;
import dk.dbc.testee.SameThreadExecutorService;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import dk.dbc.weekresolver.connector.WeekResolverConnectorException;
import dk.dbc.weekresolver.model.WeekResolverResult;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MacroSubstitutorTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    private final ManagedExecutorService managedExecutorService = new NonContainerManagedExecutorService(
            new SameThreadExecutorService());
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final BinaryFileStore binaryFileStore = mock(BinaryFileStore.class);
    private final WeekResolverConnector weekResolverConnector = mock(WeekResolverConnector.class);

    private HarvestOperation harvestOperation;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void setup() {
        environmentVariables.set("TZ", "Europe/Copenhagen");
        harvestOperation = newHarvestOperation();
    }

    @Test
    public void testNextWeekPattern() {
        // Note: This pattern does not use the week-resolver, it only calculates weeks from the given date

        final String inputQuery = "term.kk:${__NEXTWEEK_BKM__} OR term.kk:${__NEXTWEEK_ACC__} OR term.kk:${__NEXTWEEK_DPF__}";
        final String expectedQuery = "term.kk:BKM202325 OR term.kk:ACC202325 OR term.kk:DPF202325";

        ZonedDateTime now = Instant.parse("2023-06-16T12:00:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), harvestOperation::catalogueCodeToWeekCode);
        assertThat(macroSubstitutor.replace(inputQuery), is(expectedQuery));
    }

    @Test
    public void testWeekcodePatternMinus() throws WeekResolverConnectorException {
        // Note: This was the actual query that was reported as faulty and resulted in the modification to use
        //       the (correct) /current endpoint on the weekresolver. MS-4359.

        WeekResolverResult resultDbf = new WeekResolverResult();
        resultDbf.setWeekCode("DBF202316");
        when(weekResolverConnector.getCurrentWeekCodeForDate(eq("DBF"), any(LocalDate.class))).thenReturn(resultDbf);

        WeekResolverResult resultGbf = new WeekResolverResult();
        resultGbf.setWeekCode("GBF202316");
        when(weekResolverConnector.getCurrentWeekCodeForDate(eq("GBF"), any(LocalDate.class))).thenReturn(resultGbf);

        WeekResolverResult resultDlf = new WeekResolverResult();
        resultDlf.setWeekCode("DLF202316");
        when(weekResolverConnector.getCurrentWeekCodeForDate(eq("DLF"), any(LocalDate.class))).thenReturn(resultDlf);

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

        // Check that we call the getCurrentWeekCode() endpoint, NOT the getWeekCode() endpoint.
        // Also check that the date is calculated correctly and used with the call.
        ZonedDateTime then = Instant.parse("2023-04-19T15:29:00Z").atZone(ZoneId.of(System.getenv("TZ")));
        verify(weekResolverConnector, times(2)).getCurrentWeekCodeForDate("DBF", then.toLocalDate());
        verify(weekResolverConnector, times(2)).getCurrentWeekCodeForDate("GBF", then.toLocalDate());
        verify(weekResolverConnector, times(2)).getCurrentWeekCodeForDate("DLF", then.toLocalDate());
        verify(weekResolverConnector, never()).getWeekCodeForDate(anyString(), any(LocalDate.class));
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

    private HarvestOperation newHarvestOperation() {
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
                weekResolverConnector,
                managedExecutorService,
                rawRepoConnector));
    }
}
