package dk.dbc.dataio.cli;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JobSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

/**
 * Class managing all interactions with the dataIO flow-store needed for acceptance test operation
 */
public class FlowManager {
    public static final String FLOW_COMMIT_TMP = "flow.commit.tmp";

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowManager.class);

    private final FlowStoreServiceConnector flowStore;
    boolean foundFlowByName = false;

    public FlowManager(FlowStoreServiceConnector flowStoreServiceConnector) {
        flowStore = flowStoreServiceConnector;
    }

    public Flow getFlow(JobSpecification jobSpecification) throws FlowStoreServiceConnectorException {
        FlowBinder flowBinder = null;
        try {
            flowBinder = flowStore.getFlowBinder(jobSpecification.getPackaging(), jobSpecification.getFormat(), jobSpecification.getCharset(), jobSpecification.getSubmitterId(), jobSpecification.getDestination());
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            /* Status code "Not found" (404) probably means that a new flow is about to be created.
               It might also indicate an invalid job specification, but that case must be handled by
               the AccTestRunner. */
            if (e.getStatusCode() != 404) {
                throw new IllegalArgumentException("Error resolving flow binder for " + jobSpecification, e);
            }
            return null;
        }
        return flowStore.getFlow(flowBinder.getContent().getFlowId());
    }

    public Flow getFlow(Path jsar) throws IOException, FlowStoreServiceConnectorException {
        foundFlowByName = false;
        FlowContent flowContent = getFlowContent(jsar);
        Flow flowByName = null;
        try {
            flowByName = flowStore.findFlowByName(flowContent.getName());
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            /* Status code "Not found" (404) probably means that a new flow is about to be created or an
               existing flow is about to get its name updated, either way it is ok to continue. */
            if (e.getStatusCode() != 404) {
                throw e;
            }
        }
        if (flowByName != null) {
            foundFlowByName = true;
            return new Flow(flowByName.getId(), flowByName.getVersion(), flowContent);
        }
        return new Flow(1, 1, getFlowContent(jsar));
    }

    public FlowContent getFlowContent(Path jsar) throws IOException {
        return new FlowContent(Files.readAllBytes(jsar), new Date());
    }

    public boolean foundFlowByName() {
        return foundFlowByName;
    }

    public Integer commit(Path commitDir) throws IOException, FlowStoreServiceConnectorException {
        Path tempFile = commitDir.resolve(FLOW_COMMIT_TMP);
        if (!Files.isRegularFile(tempFile)) throw new IllegalStateException("Please run the test before committing");
        CommitTempFile tmp = new ObjectMapper().readValue(tempFile.toFile(), CommitTempFile.class);
        CommitTempFile.Action action = tmp.action;
        Flow flow = tmp.flow;
        if (action == CommitTempFile.Action.CREATE) {
            Flow flowCreated = flowStore.createFlow(flow.getContent());
            LOGGER.info("Created new flow with ID={}", flowCreated.getId());
        } else {
            flowStore.updateFlow(flow.getContent(), flow.getId(), flow.getVersion());
            LOGGER.info("Updated existing flow with ID={}", flow.getId());
        }
        Files.delete(tempFile);
        return 0;
    }

    public void createFlowCommitTmpFile(Path commitPath, Flow flow, CommitTempFile.Action action) {
        try {
            new ObjectMapper().writeValue(commitPath.resolve(FLOW_COMMIT_TMP).toFile(), new CommitTempFile(action, flow));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write temporary flow file", e);
        }
    }

    public static class CommitTempFile {
        public enum Action {
            CREATE,
            UPDATE
        }

        public final Action action;
        public final Flow flow;

        @JsonCreator
        public CommitTempFile(@JsonProperty("action") Action action, @JsonProperty("flow") Flow flow) {
            this.action = action;
            this.flow = flow;
        }
    }
}
