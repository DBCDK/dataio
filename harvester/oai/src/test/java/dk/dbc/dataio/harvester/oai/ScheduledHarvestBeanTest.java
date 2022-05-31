package dk.dbc.dataio.harvester.oai;

import dk.dbc.dataio.harvester.types.OaiHarvesterConfig;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ScheduledHarvestBeanTest {
    private final ScheduledHarvestBean scheduledHarvestBean = new ScheduledHarvestBean();

    @Test
    public void canRun_withInvalidSchedule() {
        final OaiHarvesterConfig config = new OaiHarvesterConfig(1, 2,
                new OaiHarvesterConfig.Content()
                        .withSchedule("invalid"));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }

    @Test
    public void canRun_withValidSchedule() {
        final OaiHarvesterConfig config = new OaiHarvesterConfig(1, 2,
                new OaiHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(null));

        assertThat(scheduledHarvestBean.canRun(config), is(true));
    }

    @Test
    public void canRun_withTimeOfLastHarvestTooCloseToNow() {
        final OaiHarvesterConfig config = new OaiHarvesterConfig(1, 2,
                new OaiHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(new Date()));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }
}
