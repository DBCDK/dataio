package dk.dbc.dataio.gui.client.pages.sinkcreateedit;

import dk.dbc.dataio.gui.client.presenters.Presenter;

public interface SinkCreateEditPresenter extends Presenter {
    void saveSink(String sinkName, String resourceName);
}
