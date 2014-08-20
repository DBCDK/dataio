package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

/**
 * Views are UI components from which the presenter can get or set values,
 * and they should have no application logic.
 *
 * @param <T> specific Presenter sub type
 */
public interface GenericView<T extends GenericPresenter> extends IsWidget {
    void setPresenter(T presenter);
    void setStatusText(String message);
    void setErrorText(String message);
}
