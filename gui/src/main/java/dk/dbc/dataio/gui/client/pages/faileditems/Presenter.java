package dk.dbc.dataio.gui.client.pages.faileditems;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void failedItemSelected(FailedItemModel failedItemModel);
}
