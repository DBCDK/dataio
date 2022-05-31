package dk.dbc.dataio.gui.client.pages.sink.show;

import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void editSink(SinkModel sink);

    void createSink();
}
