package dk.dbc.dataio.gui.client.pages.sink.sinksshow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.gui.client.views.GenericView;

import java.util.List;

public interface SinksShowView extends IsWidget, GenericView<SinksShowPresenter> {
    void clearFields();
    void setSinks(List<Sink> job);
}
