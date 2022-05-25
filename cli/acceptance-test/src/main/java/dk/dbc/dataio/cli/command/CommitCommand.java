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
