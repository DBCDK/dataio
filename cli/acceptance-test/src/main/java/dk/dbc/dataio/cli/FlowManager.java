package dk.dbc.dataio.cli;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.javascript.JavaScriptProject;
import dk.dbc.dataio.commons.javascript.JavaScriptProjectException;
import dk.dbc.dataio.commons.javascript.JavaScriptSubversionProject;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class managing all interactions with the dataIO flow-store needed for acceptance test operation
 */
public class FlowManager {
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final JavaScriptSubversionProject subversionProject;
    private final String flowCommitTmp = "flow.commit.tmp";
    private final JSONBContext jsonbContext;

    public FlowManager(String flowStoreEndpoint, String scmEndpoint) {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        flowStoreServiceConnector = new FlowStoreServiceConnector(client, flowStoreEndpoint);
        subversionProject = new JavaScriptSubversionProject(scmEndpoint);
        jsonbContext = new JSONBContext();
    }

    public Flow getFlow(String flowName, Long revision) throws FlowStoreServiceConnectorException, JavaScriptProjectException, IllegalStateException, IOException, JSONBException {
        final Flow flow = flowStoreServiceConnector.findFlowByName(flowName);
        if (flow.getContent().getComponents().size() > 1) {
            throw new IllegalStateException("more than one flow component referenced by flow");
        }
        validateSvnRevision(flow, revision);
        final FlowComponentContent content = flow.getContent().getComponents().get(0).getContent();
        final JavaScriptProject javaScriptProject = getJavaScriptProject(revision, content);
        final FlowComponentContent next = getNextContent(content, javaScriptProject, revision);
        flow.getContent().getComponents().get(0).withNext(next);
        createFlowCommitTmpFile(flow);
        return flow;
    }

    public Flow commit() throws IOException, JSONBException, FlowStoreServiceConnectorException {
        final Path flowPath = Paths.get(flowCommitTmp);
        final Flow flow = jsonbContext.unmarshall(StringUtil.asString(Files.readAllBytes(flowPath)), Flow.class);

        final FlowComponent updatedFlowComponent = updateFlowComponent(flow);
        final Flow updatedFlow = commit(flow, updatedFlowComponent);
        Files.delete(flowPath);
        return updatedFlow;
    }

    /*
     * Private methods
     */

    private void validateSvnRevision(Flow flow, long svnRevision) throws FlowStoreServiceConnectorException {
        final FlowComponent nestedFlowComponent = flow.getContent().getComponents().get(0);
        if (nestedFlowComponent.getContent().getSvnRevision() > svnRevision) {
            throw new IllegalStateException(String.format("flow: '%s' with svnRevision %s cannot be downgraded to svnRevision %s",
                    nestedFlowComponent.getContent().getName(),
                    nestedFlowComponent.getContent().getSvnRevision(),
                    svnRevision));
        }
        final FlowComponent flowComponent = flowStoreServiceConnector.getFlowComponent(nestedFlowComponent.getId());
        if (flowComponent.getContent().getSvnRevision() > svnRevision) {
            throw new IllegalStateException(String.format("flowComponent: '%s' with svnRevision %s cannot be downgraded to svnRevision %s",
                    flowComponent.getContent(),
                    flowComponent.getContent().getSvnRevision(),
                    svnRevision));
        }
    }

    private JavaScriptProject getJavaScriptProject(Long revision, FlowComponentContent current) throws JavaScriptProjectException {
        return subversionProject.fetchRequiredJavaScript(
                current.getSvnProjectForInvocationJavascript(),
                revision,
                current.getInvocationJavascriptName(),
                current.getInvocationMethod());
    }

    private FlowComponentContent getNextContent(FlowComponentContent current, JavaScriptProject javaScriptProject, Long revision) throws JavaScriptProjectException {
        return new FlowComponentContent(
                current.getName(),
                current.getSvnProjectForInvocationJavascript(),
                revision,
                current.getInvocationJavascriptName(),
                javaScriptProject.getJavaScripts(),
                current.getInvocationMethod(),
                current.getDescription(),
                javaScriptProject.getRequireCache());
    }


    private void createFlowCommitTmpFile(Flow flow) throws IOException, JSONBException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(flowCommitTmp)) {
            fileOutputStream.write(StringUtil.asBytes(jsonbContext.marshall(flow)));
        }
    }

    private FlowComponent updateFlowComponent(Flow flow) throws IOException, JSONBException, FlowStoreServiceConnectorException {
        final FlowComponent newFlowComponent = flow.getContent().getComponents().get(0);
        final FlowComponent persistedFlowComponent = flowStoreServiceConnector.getFlowComponent(newFlowComponent.getId());
        final FlowComponent updatedNext = flowStoreServiceConnector.updateNext(newFlowComponent.getNext(), persistedFlowComponent.getId(), persistedFlowComponent.getVersion());
        return flowStoreServiceConnector.updateFlowComponent(newFlowComponent.getNext(), updatedNext.getId(), updatedNext.getVersion());
    }

    private Flow commit(Flow flow, FlowComponent updatedFlowComponent) throws FlowStoreServiceConnectorException {
        flow.getContent().withComponents(updatedFlowComponent);
        return flowStoreServiceConnector.updateFlow(flow.getContent(), flow.getId(), flow.getVersion());
    }
}
