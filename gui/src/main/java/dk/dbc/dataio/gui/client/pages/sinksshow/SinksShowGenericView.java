package dk.dbc.dataio.gui.client.pages.sinksshow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.gui.client.views.GenericView;

import java.util.List;

public interface SinksShowGenericView extends IsWidget, GenericView<SinksShowGenericPresenter> {
    void setSinks(List<Sink> job);
}
