package dk.dbc.dataio.gui.client.pages.harvester.infomedia.show;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void createInfomediaHarvester();

    void editInfomediaHarvesterConfig(String id);
}
