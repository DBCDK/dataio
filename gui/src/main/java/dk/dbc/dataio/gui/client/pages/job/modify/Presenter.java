package dk.dbc.dataio.gui.client.pages.job.modify;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void packagingChanged(String packaging);

    void formatChanged(String format);

    void charsetChanged(String charset);

    void destinationChanged(String destination);

    void mailForNotificationAboutVerificationChanged(String mailForNotificationAboutVerification);

    void mailForNotificationAboutProcessingChanged(String mailForNotificationAboutProcessing);

    void resultMailInitialsChanged(String resultMailInitialsChanged);

    void typeChanged(JobSpecification.Type type);

    void keyPressed();

    void rerunButtonPressed();
}
