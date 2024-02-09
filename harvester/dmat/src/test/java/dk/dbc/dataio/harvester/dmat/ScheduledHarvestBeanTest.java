package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ScheduledHarvestBeanTest {
    private final ScheduledHarvestBean scheduledHarvestBean = new ScheduledHarvestBean();

    @Test
    void canRunWithInvalidSchedule() {
        final DMatHarvesterConfig config = new DMatHarvesterConfig(1, 2,
                new DMatHarvesterConfig.Content()
                        .withSchedule("invalid"));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }

    @Test
    void canRunWithValidSchedule() {
        final DMatHarvesterConfig config = new DMatHarvesterConfig(1, 2,
                new DMatHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(null));

        assertThat(scheduledHarvestBean.canRun(config), is(true));
    }

    @Test
    void canRunWithTimeOfLastHarvestTooCloseToNow() {
        final DMatHarvesterConfig config = new DMatHarvesterConfig(1, 2,
                new DMatHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(new Date()));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }

    @Test
    void canRunIsOverdue() {
        final DMatHarvesterConfig config = new DMatHarvesterConfig(1, 2,
                new DMatHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(Date.from(
                                Instant.now().minus(1, ChronoUnit.HOURS))));

        assertThat(scheduledHarvestBean.canRun(config), is(true));
    }
}
