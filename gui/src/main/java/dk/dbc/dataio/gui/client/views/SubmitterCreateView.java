package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.presenters.SubmitterCreatePresenter;

public interface SubmitterCreateView extends IsWidget, View<SubmitterCreatePresenter> {
    void setData(String name, String description);
}
