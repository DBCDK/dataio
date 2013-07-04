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
interface View extends IsWidget {

    void displayError(String message);

    void displaySuccess(String message);

    void refresh();

    void setPresenter(FlowEditView.Presenter presenter);

    public static interface Presenter {

        void reload();

        public void saveFlow(String name, String description);
    }
    
}
