/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;

/**
 *
 * @author slf
 */
interface ContentView extends IsWidget {
    void displayError(String message);
    void displaySuccess(String message);
    void refresh();
    void setPresenter(FlowCreateView.Presenter presenter);
    
    public static interface Presenter {
        void reload();
        public void saveFlow(String name, String description);
    }
    
}
