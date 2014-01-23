package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.presenters.SinksShowPresenter;
import java.util.List;

public interface SinksShowView extends IsWidget, View<SinksShowPresenter> {
    void setSinks(List<SinkContent> job);
}
