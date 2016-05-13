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

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD") // TODO: 5/11/16 Remove suppression when ready
public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    private final UshSolrHarvesterConfig config;
    private final HarvesterJobBuilder harvesterJobBuilder;

    public HarvestOperation(UshSolrHarvesterConfig config, HarvesterJobBuilder harvesterJobBuilder) throws NullPointerException {
        this.config = InvariantUtil.checkNotNullOrThrow(config, "config");
        this.harvesterJobBuilder = InvariantUtil.checkNotNullOrThrow(harvesterJobBuilder, "harvesterJobBuilder");
    }

    public int execute() throws HarvesterException {
        LOGGER.debug("Stubbed execute");
        return 0;
    }
}
