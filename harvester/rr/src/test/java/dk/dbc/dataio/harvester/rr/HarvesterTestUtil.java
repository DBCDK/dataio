/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.harvester.types.RRHarvesterConfig;

import java.util.ArrayList;
import java.util.List;

public class HarvesterTestUtil {
    private HarvesterTestUtil() {}

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
