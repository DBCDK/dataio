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

package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.dto.DataSet;

/**
 * Class used as a container for DataSet and TickleRepoHarvesterConfig.
 * Due to gwt directly using the backend objects, the DataSet could not be included in TickleRepoHarvesterConfig
 */
public class ExtendedTickleRepoHarvesterConfig {

    private TickleRepoHarvesterConfig tickleRepoHarvesterConfig;
    private DataSet dataSet;

    public TickleRepoHarvesterConfig getTickleRepoHarvesterConfig() {
        return tickleRepoHarvesterConfig;
    }

    public ExtendedTickleRepoHarvesterConfig withTickleRepoHarvesterConfig(TickleRepoHarvesterConfig tickleRepoHarvesterConfig) {
        this.tickleRepoHarvesterConfig = tickleRepoHarvesterConfig;
        return this;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public ExtendedTickleRepoHarvesterConfig withDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
        return this;
    }
}
