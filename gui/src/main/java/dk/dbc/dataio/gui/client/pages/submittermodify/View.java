package dk.dbc.dataio.gui.client.pages.submittermodify;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.views.GenericView;

public interface View extends IsWidget, GenericView<Presenter> {
    void setNumber(String number);
    String getNumber();
    void setName(String name);
    String getName();
    void setDescription(String description);
    String getDescription();
}
