package dk.dbc.dataio.gui.client.pages.job.show;

import dk.dbc.dataio.gui.util.ClientFactory;

public class TestJobsView extends View {
    /**
     * Default constructor
     *
     * @param clientFactory Clientfactory to be used in the View
     */
    public TestJobsView(ClientFactory clientFactory) {
        super(clientFactory, clientFactory.getMenuTexts().menu_TestJobs());
    }
}
