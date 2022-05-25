package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.harvester.types.RRHarvesterConfig;

import java.util.ArrayList;
import java.util.List;

public class HarvesterTestUtil {
    private HarvesterTestUtil() {
    }

    public static List<RRHarvesterConfig> getRRHarvesterConfigs(RRHarvesterConfig.Content... entries) {
        final List<RRHarvesterConfig> configs = new ArrayList<>(entries.length);
        long id = 1;
        for (RRHarvesterConfig.Content content : entries) {
            configs.add(new RRHarvesterConfig(id++, 1, content));
        }
        return configs;
    }

    public static RRHarvesterConfig getRRHarvesterConfig(RRHarvesterConfig.Content content) {
        return new RRHarvesterConfig(1, 1, content);
    }

    public static RRHarvesterConfig getRRHarvesterConfig() {
        return getRRHarvesterConfig(getRRHarvestConfigContent());
    }

    public static RRHarvesterConfig.Content getRRHarvestConfigContent() {
        return new RRHarvesterConfig.Content()
                .withId("id")
                .withResource("resource")
                .withConsumerId("consumerId")
                .withFormat("format")
                .withDestination("destination")
                .withIncludeRelations(false);
    }
}
