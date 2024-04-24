package dk.dbc.dataio.cli;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.javascript.JavaScriptProject;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JobSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Class managing all interactions with the dataIO flow-store needed for acceptance test operation
 */
public class FlowManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowManager.class);
    private final FlowStoreServiceConnector flowStore;
    private static final String FLOW_COMMIT_TMP = "flow.commit.tmp";
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    public FlowManager(FlowStoreServiceConnector flowStoreServiceConnector) {
        flowStore = flowStoreServiceConnector;
    }

    public Flow getFlow(JobSpecification jobSpecification) throws FlowStoreServiceConnectorException {
        FlowBinder flowBinder = flowStore.getFlowBinder(jobSpecification.getPackaging(), jobSpecification.getFormat(), jobSpecification.getCharset(), jobSpecification.getSubmitterId(), jobSpecification.getDestination());
        return flowStore.getFlow(flowBinder.getContent().getFlowId());
    }

    public Integer commit(JavaScriptProject project) throws IOException, FlowStoreServiceConnectorException {
        Path tempFile = Path.of(FLOW_COMMIT_TMP);
        if(!Files.isRegularFile(tempFile)) throw new IllegalStateException("Please run the test before committing the version");
        CommitTempFile tmp = new ObjectMapper().readValue(tempFile.toFile(), CommitTempFile.class);
        Flow flow = tmp.flow;
        Long revision = tmp.version;
        validateSvnRevision(flow, revision);
        FlowComponent updatedFlowComponent = updateFlowComponent(flow, project, revision);
        Flow updatedFlow = commit(flow, updatedFlowComponent);
        Files.delete(tempFile);
        return 0;
    }

    public void createFlowCommitTmpFile(Flow flow, Long version) {
        try {
            new ObjectMapper().writeValue(new File(FLOW_COMMIT_TMP), new CommitTempFile(version, flow));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write temporary flow file", e);
        }
    }

    private void validateSvnRevision(Flow flow, long svnRevision) throws FlowStoreServiceConnectorException {
        FlowComponent nestedFlowComponent = flow.getContent().getComponents().get(0);
        if (nestedFlowComponent.getContent().getSvnRevision() > svnRevision) {
            throw new IllegalStateException(String.format("flow: '%s' with svnRevision %s cannot be downgraded to svnRevision %s",
                    nestedFlowComponent.getContent().getName(),
                    nestedFlowComponent.getContent().getSvnRevision(),
                    svnRevision));
        }
//        FlowComponent flowComponent = flowStore.getFlowComponent(nestedFlowComponent.getId());
//        if (flowComponent.getContent().getSvnRevision() > svnRevision) {
//            throw new IllegalStateException(String.format("flowComponent: '%s' with svnRevision %s cannot be downgraded to svnRevision %s",
//                    flowComponent.getContent(),
//                    flowComponent.getContent().getSvnRevision(),
//                    svnRevision));
//        }
    }

    private FlowComponentContent getNextContent(FlowComponentContent current, JavaScriptProject project, Long revision) {
        return new FlowComponentContent(
                current.getName(),
                current.getSvnProjectForInvocationJavascript(),
                revision,
                current.getInvocationJavascriptName(),
                project.getJavaScripts(),
                current.getInvocationMethod(),
                current.getDescription(),
                project.getRequireCache());
    }

    private FlowComponent updateFlowComponent(Flow flow, JavaScriptProject project, Long revision) throws FlowStoreServiceConnectorException {
        FlowComponent component = flow.getContent().getComponents().get(0);
        FlowComponentContent content = getNextContent(component.getContent(), project, revision);
        LOGGER.info("Updating flow component {}", component.getId());
        return flowStore.updateFlowComponent(content, component.getId(), component.getVersion());
    }

    private Flow commit(Flow flow, FlowComponent updatedFlowComponent) throws FlowStoreServiceConnectorException {
        LOGGER.info("Committing flow {} - {}", flow.getId(), flow.getContent().getName());
        flow.getContent().withComponents(updatedFlowComponent);
        return flowStore.updateFlow(flow.getContent(), flow.getId(), flow.getVersion());
    }

    public static class CommitTempFile {
        public final Long version;
        public final Flow flow;

        @JsonCreator
        public CommitTempFile(@JsonProperty("version") Long version, @JsonProperty("flow") Flow flow) {
            this.version = version;
            this.flow = flow;
        }
    }
}
