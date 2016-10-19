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
import dk.dbc.dataio.cli.SubversionManager;
import dk.dbc.dataio.cli.UrlManager;
import dk.dbc.dataio.cli.options.CreateOptions;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.javascript.JavaScriptProject;
import dk.dbc.dataio.commons.javascript.JavaScriptProjectException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
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
    private FlowManager flowManager;
    private SubversionManager subversionManager;

    public CreateCommand(CreateOptions options) {
        this.options = options;
    }

    @Override
    public void execute() throws FlowStoreServiceConnectorException, UrlResolverServiceConnectorException, JavaScriptProjectException {
        initializeManagers();
        LOGGER.info("Looking up flow '{}'", options.flowName);
        final Flow flow = flowManager.getFlow(options.flowName);
        LOGGER.debug("found flow with id {} and version {}", flow.getId(), flow.getVersion());
        final FlowComponentContent current = flow.getContent().getComponents().get(0).getContent();
        LOGGER.info("Extracting Javascript project with from subversion");
        final JavaScriptProject javaScriptProject = subversionManager.getJavaScriptProject(options.revision, current);
        LOGGER.info("creating next content with revision '{}'", options.revision);
        final FlowComponentContent next = flowManager.getNextContent(current, javaScriptProject, options.revision);
        LOGGER.info("Updating flow with next content");
        final Flow updatedFlow = flowManager.updateFlow(flow, next);
        LOGGER.debug("flow with id {} is now at version {}", updatedFlow.getId(), updatedFlow.getVersion());
    }

    /*
     * Private methods
     */

    private void initializeManagers() throws UrlResolverServiceConnectorException {
        LOGGER.info("initializing UrlManager using endpoint: {}", options.guiUrl);
        final UrlManager urlManager = new UrlManager(options.guiUrl);
        final Map<String, String> urls = urlManager.getUrls();

        LOGGER.info("initializing FlowManager using endpoint: {}", urls.get(JndiConstants.FLOW_STORE_SERVICE_ENDPOINT_RESOURCE));
        flowManager = new FlowManager(urls.get(JndiConstants.FLOW_STORE_SERVICE_ENDPOINT_RESOURCE));

        LOGGER.info("initializing SubversionManager using endpoint: {}", urls.get(JndiConstants.SUBVERSION_SCM_ENDPOINT_RESOURCE));
        subversionManager = new SubversionManager(urls.get(JndiConstants.SUBVERSION_SCM_ENDPOINT_RESOURCE));
    }

}
