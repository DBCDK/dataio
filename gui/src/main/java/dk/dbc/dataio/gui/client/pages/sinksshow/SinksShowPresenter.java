package dk.dbc.dataio.gui.client.pages.sinksshow;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.gui.client.presenters.Presenter;

public interface SinksShowPresenter extends Presenter {
    void editSink(Sink sink);
}
