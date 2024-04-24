package dk.dbc.dataio.cli;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.javascript.JavaScriptProject;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.lang.StringUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class managing all interactions with the dataIO flow-store needed for acceptance test operation
 */
public class FlowManager {
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

    public Flow commit(JavaScriptProject project, Long revision) throws IOException, JSONBException, FlowStoreServiceConnectorException {
        Path flowPath = Paths.get(FLOW_COMMIT_TMP);
        Flow flow = JSONB_CONTEXT.unmarshall(StringUtil.asString(Files.readAllBytes(flowPath)), Flow.class);
        validateSvnRevision(flow, revision);
        FlowComponent updatedFlowComponent = updateFlowComponent(flow, project, revision);
        Flow updatedFlow = commit(flow, updatedFlowComponent);
        Files.delete(flowPath);
        return updatedFlow;
    }

    public void createFlowCommitTmpFile(Flow flow) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(FLOW_COMMIT_TMP)) {
            fileOutputStream.write(StringUtil.asBytes(JSONB_CONTEXT.marshall(flow)));
        } catch (IOException | JSONBException e) {
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
        FlowComponent flowComponent = flowStore.getFlowComponent(nestedFlowComponent.getId());
        if (flowComponent.getContent().getSvnRevision() > svnRevision) {
            throw new IllegalStateException(String.format("flowComponent: '%s' with svnRevision %s cannot be downgraded to svnRevision %s",
                    flowComponent.getContent(),
                    flowComponent.getContent().getSvnRevision(),
                    svnRevision));
        }
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
        FlowComponent oldComponent = flow.getContent().getComponents().get(0);
        FlowComponentContent content = oldComponent.getContent();
        FlowComponentContent newContent = getNextContent(content, project, revision);
        return flowStore.updateFlowComponent(newContent, oldComponent.getId(), oldComponent.getVersion());
    }

    private Flow commit(Flow flow, FlowComponent updatedFlowComponent) throws FlowStoreServiceConnectorException {
        flow.getContent().withComponents(updatedFlowComponent);
        return flowStore.updateFlow(flow.getContent(), flow.getId(), flow.getVersion());
    }
}
