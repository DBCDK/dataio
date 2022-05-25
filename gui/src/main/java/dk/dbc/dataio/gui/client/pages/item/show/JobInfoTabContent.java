package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.prompted.PromptedAnchor;
import dk.dbc.dataio.gui.client.components.prompted.PromptedHyperlink;
import dk.dbc.dataio.gui.client.components.prompted.PromptedLabel;

public class JobInfoTabContent extends Composite {
    interface JobInfoTabContentUiBinder extends UiBinder<HTMLPanel, JobInfoTabContent> {
    }

    private static JobInfoTabContentUiBinder ourUiBinder = GWT.create(JobInfoTabContentUiBinder.class);

    public JobInfoTabContent() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiField
    PromptedLabel packaging;
    @UiField
    PromptedLabel format;
    @UiField
    PromptedLabel charset;
    @UiField
    PromptedLabel destination;
    @UiField
    PromptedLabel mailForNotificationAboutVerification;
    @UiField
    PromptedLabel mailForNotificationAboutProcessing;
    @UiField
    PromptedLabel resultMailInitials;
    @UiField
    PromptedLabel type;
    @UiField
    PromptedLabel jobCreationTime;
    @UiField
    PromptedLabel jobCompletionTime;
    @UiField
    PromptedHyperlink previousJobId;
    @UiField
    Label exportLinksHeader;
    @UiField
    PromptedAnchor exportLinkItemsPartitioned;
    @UiField
    PromptedAnchor exportLinkItemsProcessed;
    @UiField
    PromptedAnchor exportLinkItemsFailedInPartitioning;
    @UiField
    PromptedAnchor exportLinkItemsFailedInProcessing;
    @UiField
    PromptedAnchor exportLinkItemsFailedInDelivering;
    @UiField
    PromptedAnchor fileStore;
    @UiField
    HTMLPanel ancestrySection;
    @UiField
    PromptedLabel ancestryTransFile;
    @UiField
    PromptedLabel ancestryDataFile;
    @UiField
    PromptedLabel ancestryBatchId;
    @UiField
    InlineHTML ancestryContent;
}
