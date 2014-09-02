package dk.dbc.dataio.gui.client.pages.submittermodify;

/**
 *
 * This is the implementation of the Submitter Edit View
 *
 */
public class ViewEditImpl extends ViewImpl {

    /**
     * Constructor
     */
    public ViewEditImpl(String header, SubmitterModifyConstants constants) {
        super(header, constants);
        numberPanel.setEnabled(false);
    }

}

