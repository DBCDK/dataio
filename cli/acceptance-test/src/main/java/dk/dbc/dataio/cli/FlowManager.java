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

package dk.dbc.dataio.cli;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.javascript.JavaScriptProject;
import dk.dbc.dataio.commons.javascript.JavaScriptProjectException;
import dk.dbc.dataio.commons.javascript.JavaScriptSubversionProject;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
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
        if(flow.getContent().getComponents().size() > 1) {
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
        final Flow updatedFlow = commit(flow);
        updateFlowComponent(flow);
        Files.delete(flowPath);
        return updatedFlow;
    }

    /*
     * Private methods
     */

    private void validateSvnRevision(Flow flow, long svnRevision) throws FlowStoreServiceConnectorException {
        final FlowComponent nestedFlowComponent = flow.getContent().getComponents().get(0);
        if(nestedFlowComponent.getContent().getSvnRevision() > svnRevision) {
            throw new IllegalStateException(String.format("flow: '%s' with svnRevision %s cannot be downgraded to svnRevision %s",
                    nestedFlowComponent.getContent().getName(),
                    nestedFlowComponent.getContent().getSvnRevision(),
                    svnRevision));
        }
        final FlowComponent flowComponent = flowStoreServiceConnector.getFlowComponent(nestedFlowComponent.getId());
        if(flowComponent.getContent().getSvnRevision() > svnRevision) {
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

    private void updateFlowComponent(Flow flow) throws IOException, JSONBException, FlowStoreServiceConnectorException {
        final FlowComponent flowComponent = flow.getContent().getComponents().get(0);
        flowStoreServiceConnector.updateFlowComponent(flowComponent.getNext(), flowComponent.getId(), flowComponent.getVersion());
    }

    private Flow commit(Flow flow) throws FlowStoreServiceConnectorException {
        final FlowContent flowContent = flow.getContent();
        flowContent.getComponents().get(0).withContent(flowContent.getComponents().get(0).getNext());
        return flowStoreServiceConnector.updateFlow(flowContent, flow.getId(), flow.getVersion());
    }
}