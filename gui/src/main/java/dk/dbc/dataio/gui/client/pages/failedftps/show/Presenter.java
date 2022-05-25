package dk.dbc.dataio.gui.client.pages.failedftps.show;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;
import dk.dbc.dataio.jobstore.types.Notification;

public interface Presenter extends GenericPresenter {
    void showTransFileContent(Notification notification);

    void resendFtp(String transFileName, String transFileContent);
}
