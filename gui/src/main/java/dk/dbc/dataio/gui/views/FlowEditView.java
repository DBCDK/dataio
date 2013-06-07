/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.views;

import com.google.gwt.user.client.ui.IsWidget;

/**
 *
 * @author slf
 */
public interface FlowEditView extends IsWidget {

    public void setData(String name, String description);

    public void refresh();

    public void displayError(String message);

    public void displaySuccess(String message);

    public void setPresenter(Presenter presenter);

    interface Presenter {

        void reload();

        public void saveFlow(String name, String description);
    }
}
