/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;

public class FlowStoreServiceWiremockRecorder {
    private final FlowStoreServiceConnector flowStoreServiceConnector;

    /*
            Steps to reproduce wiremock recording:

            - Start standalone runner
                java -jar wiremock-[WIRE_MOCK_VERSION]-standalone.jar --proxy-all="[FLOW_STORE_SERVICE_URL]" --record-mappings --verbose

            - Run the main method of this class

            - Replace content of src/test/resources/{__files|mappings} with that produced by the standalone runner
         */
    public static void main(String[] args) throws FlowStoreServiceConnectorException {
        final FlowStoreServiceWiremockRecorder recorder = new FlowStoreServiceWiremockRecorder();
        recorder.lookupEntities("addi-xml", "basis", "utf8", "870970", "broend-cisterne");
        recorder.lookupEntities("addi-xml", "periode", "utf8", "876070", "test");
    }

    private FlowStoreServiceWiremockRecorder() {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        flowStoreServiceConnector = new FlowStoreServiceConnector(client, "http://localhost:8080");
    }

    private void lookupEntities(String packaging, String format, String charset, String submitterNumber,
                                String destination) throws FlowStoreServiceConnectorException {
        final FlowBinder flowBinder = flowStoreServiceConnector.getFlowBinder(
                packaging, format, charset, Long.parseLong(submitterNumber), destination);
        flowStoreServiceConnector.getFlow(flowBinder.getContent().getFlowId());
        flowStoreServiceConnector.getSink(flowBinder.getContent().getSinkId());
        final Submitter submitter = flowStoreServiceConnector.getSubmitterBySubmitterNumber(Long.parseLong(submitterNumber));
        flowStoreServiceConnector.getSubmitter(submitter.getId());
    }
}
