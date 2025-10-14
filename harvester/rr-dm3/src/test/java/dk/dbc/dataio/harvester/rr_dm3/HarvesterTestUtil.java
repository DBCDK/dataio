package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.harvester.types.RRV3HarvesterConfig;

import java.util.ArrayList;
import java.util.List;

public class HarvesterTestUtil {
    private HarvesterTestUtil() {
    }

    public static List<RRV3HarvesterConfig> getRRHarvesterConfigs(RRV3HarvesterConfig.Content... entries) {
        List<RRV3HarvesterConfig> configs = new ArrayList<>(entries.length);
        long id = 1;
        for (RRV3HarvesterConfig.Content content : entries) {
            configs.add(new RRV3HarvesterConfig(id++, 1, content));
        }
        return configs;
    }

    public static RRV3HarvesterConfig getRRHarvesterConfig(RRV3HarvesterConfig.Content content) {
        return new RRV3HarvesterConfig(1, 1, content);
    }

    public static RRV3HarvesterConfig getRRHarvesterConfig() {
        return getRRHarvesterConfig(getRRHarvestConfigContent());
    }

    public static RRV3HarvesterConfig.Content getRRHarvestConfigContent() {
        return new RRV3HarvesterConfig.Content()
                .withId("id")
                .withResource("resource")
                .withConsumerId("consumerId")
                .withFormat("format")
                .withDestination("destination")
                .withIncludeRelations(false);
    }
}
