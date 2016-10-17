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

package dk.dbc.dataio.cli.command;

import dk.dbc.dataio.cli.FlowManager;
import dk.dbc.dataio.cli.UrlManager;
import dk.dbc.dataio.cli.options.CreateOptions;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Acceptance test command line interface for 'create' sub command
 */
public class CreateCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCommand.class);

    private final CreateOptions options;
    private final FlowManager flowManager;
    private final UrlManager urlManager;

    public CreateCommand(CreateOptions options) {
        this.options = options;
        flowManager = new FlowManager(options.flowStoreUrl);
        urlManager = new UrlManager(options.guiUrl);
    }

    @Override
    public void execute() throws FlowStoreServiceConnectorException, UrlResolverServiceConnectorException {
        LOGGER.info("Looking up flow '{}'", options.flowName);
        final Flow flow = flowManager.getFlow(options.flowName);
        LOGGER.info("found {}", flow.getId());
        LOGGER.info("Looking up endpoints");
        final Map<String, String> endpoints = urlManager.getUrls();
        LOGGER.info("found {}", endpoints.toString());
    }
}
