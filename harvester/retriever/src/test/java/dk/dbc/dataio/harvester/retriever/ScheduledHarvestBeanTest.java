package dk.dbc.dataio.harvester.retriever;

import dk.dbc.dataio.harvester.types.RetrieverHarvesterConfig;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ScheduledHarvestBeanTest {
    private final ScheduledHarvestBean scheduledHarvestBean = new ScheduledHarvestBean();

    @Test
    public void canRun_withInvalidSchedule() {
        RetrieverHarvesterConfig config = new RetrieverHarvesterConfig(1, 2,
                new RetrieverHarvesterConfig.Content()
                        .withSchedule("invalid"));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }

    @Test
    public void canRun_withValidSchedule() {
        RetrieverHarvesterConfig config = new RetrieverHarvesterConfig(1, 2,
                new RetrieverHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(null));

        assertThat(scheduledHarvestBean.canRun(config), is(true));
    }

    @Test
    public void canRun_withTimeOfLastHarvestTooCloseToNow() {
        RetrieverHarvesterConfig config = new RetrieverHarvesterConfig(1, 2,
                new RetrieverHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(new Date()));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }
}
