package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void createPeriodicJobsHarvester();

    void editPeriodicJobsHarvester(String id);
}
