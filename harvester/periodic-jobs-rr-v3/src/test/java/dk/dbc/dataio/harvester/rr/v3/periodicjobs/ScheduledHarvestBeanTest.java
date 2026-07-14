package dk.dbc.dataio.harvester.rr.v3.periodicjobs;

import dk.dbc.dataio.harvester.types.PeriodicJobsV3HarvesterConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ScheduledHarvestBeanTest {
    private final ScheduledHarvestBean scheduledHarvestBean = new ScheduledHarvestBean();

    @Test
    public void canRun_withInvalidSchedule() {
        PeriodicJobsV3HarvesterConfig config = new PeriodicJobsV3HarvesterConfig(1, 2,
                new PeriodicJobsV3HarvesterConfig.Content()
                        .withSchedule("invalid"));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }

    @Test
    public void canRun_withValidSchedule() {
        PeriodicJobsV3HarvesterConfig config = new PeriodicJobsV3HarvesterConfig(1, 2,
                new PeriodicJobsV3HarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(null));

        assertThat(scheduledHarvestBean.canRun(config), is(true));
    }

    @Test
    public void canRun_withTimeOfLastHarvestTooCloseToNow() {
        PeriodicJobsV3HarvesterConfig config = new PeriodicJobsV3HarvesterConfig(1, 2,
                new PeriodicJobsV3HarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(new Date()));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }

    @Test
    public void canRun_isOverdue() {
        PeriodicJobsV3HarvesterConfig config = new PeriodicJobsV3HarvesterConfig(1, 2,
                new PeriodicJobsV3HarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(Date.from(
                                Instant.now().minus(1, ChronoUnit.HOURS))));

        assertThat(scheduledHarvestBean.canRun(config), is(true));
    }
}
