package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
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
        PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withSchedule("invalid"));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }

    @Test
    public void canRun_withValidSchedule() {
        PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(null));

        assertThat(scheduledHarvestBean.canRun(config), is(true));
    }

    @Test
    public void canRun_withTimeOfLastHarvestTooCloseToNow() {
        PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(new Date()));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }

    @Test
    public void canRun_isOverdue() {
        PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(Date.from(
                                Instant.now().minus(1, ChronoUnit.HOURS))));

        assertThat(scheduledHarvestBean.canRun(config), is(true));
    }
}
