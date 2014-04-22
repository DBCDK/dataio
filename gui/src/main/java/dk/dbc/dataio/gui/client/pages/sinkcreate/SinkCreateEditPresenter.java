package dk.dbc.dataio.gui.client.pages.sinkcreate;

import dk.dbc.dataio.gui.client.presenters.Presenter;

public interface SinkCreateEditPresenter extends Presenter {
    void saveSink(String sinkName, String resourceName);
}
