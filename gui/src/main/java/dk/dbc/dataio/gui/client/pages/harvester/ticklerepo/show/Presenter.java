package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.show;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void createTickleRepoHarvester();

    void editTickleRepoHarvesterConfig(String id);
}
