package dk.dbc.dataio.gui.client.pages.sinkcreateedit;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface SinkCreateEditGenericPresenter extends GenericPresenter {
    void saveSink(String sinkName, String resourceName);
}
