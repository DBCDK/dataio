package dk.dbc.dataio.gui.client.views;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

/**
 * Views are UI components from which the presenter can get or set values,
 * and they should have no application logic.
 *
 * @param T specific Presenter sub type
 */
public interface GenericView<T extends GenericPresenter> {
    void refresh();
    void clearFields();
    void setPresenter(T presenter);
    void onSuccess(String message);
    void onFailure(String message);
}
