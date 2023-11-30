package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ScheduledHarvestBeanTest {
    private final ScheduledHarvestBean scheduledHarvestBean = new ScheduledHarvestBean();

    @Test
    public void canRun_withInvalidSchedule() {
        InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(1, 2,
                new InfomediaHarvesterConfig.Content()
                        .withSchedule("invalid"));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }

    @Test
    public void canRun_withValidSchedule() {
        InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(1, 2,
                new InfomediaHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(null));

        assertThat(scheduledHarvestBean.canRun(config), is(true));
    }

    @Test
    public void canRun_withTimeOfLastHarvestTooCloseToNow() {
        InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(1, 2,
                new InfomediaHarvesterConfig.Content()
                        .withSchedule("* * * * *")
                        .withTimeOfLastHarvest(new Date()));

        assertThat(scheduledHarvestBean.canRun(config), is(false));
    }
}
