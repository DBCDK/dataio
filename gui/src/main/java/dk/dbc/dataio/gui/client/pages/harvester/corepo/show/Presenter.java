package dk.dbc.dataio.gui.client.pages.harvester.corepo.show;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void createCoRepoHarvester();

    void editCoRepoHarvesterConfig(String id);
}
