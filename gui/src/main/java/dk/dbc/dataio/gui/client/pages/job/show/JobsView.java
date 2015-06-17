package dk.dbc.dataio.gui.client.pages.job.show;

import dk.dbc.dataio.gui.util.ClientFactory;

public class JobsView extends View {
    /**
     * Default constructor
     *
     * @param clientFactory Clientfactory to be used in the View
     */
    public JobsView(ClientFactory clientFactory) {
        super(clientFactory, clientFactory.getMenuTexts().menu_Jobs());
    }
}
