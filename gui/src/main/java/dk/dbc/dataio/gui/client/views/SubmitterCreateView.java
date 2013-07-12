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
public interface SubmitterCreateView extends IsWidget {

    public void setData(String name, String description);

    void displayError(String message);

    void displaySuccess(String message);

    void refresh();

    void setPresenter(Presenter presenter);

    public static interface Presenter {

        void reload();

        public void saveSubmitter(String name, String number, String description);
    }

}
