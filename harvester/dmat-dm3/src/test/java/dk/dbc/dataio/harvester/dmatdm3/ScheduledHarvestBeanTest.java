package dk.dbc.dataio.harvester.dmatdm3;

import dk.dbc.dataio.harvester.types.DMatDM3HarvesterConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ScheduledHarvestBeanTest {
    private final ScheduledHarvestBean scheduledHarvestBean = new ScheduledHarvestBean();

    @Test
    void canRunWithInvalidSchedule() {
        final DMatDM3HarvesterConfig config = new DMatDM3HarvesterConfig(1, 2,
                new DMatDM3HarvesterConfig.Content()
                        .withSchedule("invalid"));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }

    @Test
    void canRunWithValidSchedule() {
        final DMatDM3HarvesterConfig config = new DMatDM3HarvesterConfig(1, 2,
                new DMatDM3HarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(null));

        assertThat(scheduledHarvestBean.canRun(config), is(true));
    }

    @Test
    void canRunWithTimeOfLastHarvestTooCloseToNow() {
        final DMatDM3HarvesterConfig config = new DMatDM3HarvesterConfig(1, 2,
                new DMatDM3HarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(new Date()));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }

    @Test
    void canRunIsOverdue() {
        final DMatDM3HarvesterConfig config = new DMatDM3HarvesterConfig(1, 2,
                new DMatDM3HarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(Date.from(
                                Instant.now().minus(1, ChronoUnit.HOURS))));

        assertThat(scheduledHarvestBean.canRun(config), is(true));
    }
}
