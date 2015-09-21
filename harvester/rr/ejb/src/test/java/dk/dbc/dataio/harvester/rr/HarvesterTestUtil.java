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
                .setFormat("format")
                .setDestination("destination")
                .setIncludeRelations(false);
    }
}
