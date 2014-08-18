package dk.dbc.dataio.gui.client.presenters;

/**
 * Presenters are where the application logic sits, and they have no UI logic.
 */
public interface GenericPresenter {
    /**
     * This is where the presenter will listen to any application wide events it is
     * interested in as well as where it calls the associated view's setPresenter
     * method to bind the view to itself.
     */
    void bind();
}
