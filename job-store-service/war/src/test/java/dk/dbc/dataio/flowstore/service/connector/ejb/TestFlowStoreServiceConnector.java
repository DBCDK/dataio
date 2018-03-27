
package dk.dbc.dataio.flowstore.service.connector.ejb;


import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;

import javax.ws.rs.ProcessingException;

public class TestFlowStoreServiceConnector extends FlowStoreServiceConnector {

    static Submitter submitter = new SubmitterBuilder().build();

    public static void updateSubmitter(SubmitterContent submitterContent) {
        submitter = new SubmitterBuilder().setContent(submitterContent).build();
    }

    public TestFlowStoreServiceConnector() {
        super(HttpClient.newClient(), "baseUrl");
    }

    @Override
    public Submitter createSubmitter(SubmitterContent content) {
        throw new ProcessingException("Test Connector unable to create submitter ");
    }

    @Override
    public Submitter getSubmitter(long submitterId) {
        return submitter;
    }

    @Override
    public void deleteSubmitter(long submitterId, long version) {
        throw new ProcessingException("Test connector unable to delete submitter");
    }
}
