package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;

public class HarvesterTestUtil {
    private HarvesterTestUtil() {}

    public static RawRepoHarvesterConfig getRawRepoHarvesterConfig(RawRepoHarvesterConfig.Entry... entries) {
        final RawRepoHarvesterConfig rawRepoHarvesterConfig = new RawRepoHarvesterConfig();
        for (RawRepoHarvesterConfig.Entry entry : entries) {
            rawRepoHarvesterConfig.addEntry(entry);
        }
        return rawRepoHarvesterConfig;
    }

    public static RawRepoHarvesterConfig.Entry getHarvestOperationConfigEntry() {
        return new RawRepoHarvesterConfig.Entry()
                .setId("id")
                .setResource("resource")
                .setConsumerId("consumerId")
                .setDestination("destination");
    }
}
