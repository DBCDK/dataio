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

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.cli.FlowManager;
import dk.dbc.dataio.cli.options.CommitOptions;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class CommitCommand extends Command<CommitOptions> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitCommand.class);
    private FlowManager flowManager;

    public CommitCommand(CommitOptions options) {
        super(options);
    }

    @Override
    public void execute() throws IOException, JSONBException, UrlResolverServiceConnectorException, FlowStoreServiceConnectorException {
        initializeManagers();
        LOGGER.info("updating flow and flow component referenced by flow");
        final Flow flow = flowManager.commit();
        final FlowComponent flowComponent = flow.getContent().getComponents().get(0);
        LOGGER.debug("Successfully updated flow {} and referenced flow component {} to svn revision {}",
                flow.getId(),
                flowComponent.getId(),
                flowComponent.getContent().getSvnRevision());
    }

    private void initializeManagers() throws UrlResolverServiceConnectorException {
        LOGGER.info("Retrieving endpoints using {}", options.guiUrl);
        final Map<String, String> endpoints = getEndpoints();

        LOGGER.info("initializing FlowManager");
        flowManager = new FlowManager(
                endpoints.get("FLOWSTORE_URL"),
                endpoints.get("SUBVERSION_URL"));
    }
}