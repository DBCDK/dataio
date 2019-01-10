/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;

public class HarvestOperation {
    private final InfomediaHarvesterConfig config;
    private final FlowStoreServiceConnector flowStoreServiceConnector;

    public HarvestOperation(InfomediaHarvesterConfig config, FlowStoreServiceConnector flowStoreServiceConnector) {
        this.config = config;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
    }

    public int execute() throws HarvesterException {
        // TODO: 10-01-19 to be implemented
        return 0;
    }
}
