package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import dk.dbc.dataio.gui.client.components.PromptedLabel;

public class JobInfoTabContent extends Composite {
    interface JobInfoTabContentUiBinder extends UiBinder<HTMLPanel, JobInfoTabContent> {
    }

    private static JobInfoTabContentUiBinder ourUiBinder = GWT.create(JobInfoTabContentUiBinder.class);

    public JobInfoTabContent() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiField PromptedLabel packaging;
    @UiField PromptedLabel format;
    @UiField PromptedLabel charset;
    @UiField PromptedLabel destination;
    @UiField PromptedLabel mailForNotificationAboutVerification;
    @UiField PromptedLabel mailForNotificationAboutProcessing;
    @UiField PromptedLabel resultMailInitials;

}