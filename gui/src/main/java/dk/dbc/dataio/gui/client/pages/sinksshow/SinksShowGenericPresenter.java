package dk.dbc.dataio.gui.client.pages.sinksshow;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface SinksShowGenericPresenter extends GenericPresenter {
    void editSink(Sink sink);
}
