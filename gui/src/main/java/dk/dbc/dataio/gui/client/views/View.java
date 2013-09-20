package dk.dbc.dataio.gui.client.views;

import dk.dbc.dataio.gui.client.presenters.Presenter;

/**
 * Views are UI components from which the presenter can get or set values,
 * and they should have no application logic.
 *
 * @param T specific Presenter sub type
 */
public interface View<T extends Presenter> {
    void onSaveFlowbinderFailure(String message);

    void displaySuccess(String message);

    void refresh();

    void setPresenter(T presenter);
}
