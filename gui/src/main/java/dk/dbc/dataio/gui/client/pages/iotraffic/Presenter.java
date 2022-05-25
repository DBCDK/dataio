package dk.dbc.dataio.gui.client.pages.iotraffic;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void submitterChanged(String submitter);

    void packagingChanged(String submitter);

    void formatChanged(String format);

    void destinationChanged(String submitter);

    void addButtonPressed();

    void deleteButtonPressed(long gatekeeperId);
}
