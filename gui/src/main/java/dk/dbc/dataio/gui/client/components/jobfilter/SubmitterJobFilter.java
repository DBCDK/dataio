package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.inject.Inject;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.resources.Resources;

/**
 * Created by ja7 on 19-08-15.
 * This is the Submitter Job Filter
 */
public class SubmitterJobFilter extends BaseJobFilter {
    interface SubmitterJobFilterUiBinder extends UiBinder<HTMLPanel, SubmitterJobFilter> {
    }

    private static SubmitterJobFilterUiBinder ourUiBinder = GWT.create(SubmitterJobFilterUiBinder.class);

    @UiConstructor
    public SubmitterJobFilter() {
        this((Texts) GWT.create(Texts.class), (Resources) GWT.create(Resources.class) );
    }

    @Inject
    public SubmitterJobFilter(Texts texts, Resources resources) {
        super(texts, resources);
        initWidget(ourUiBinder.createAndBindUi(this));
        jobListCriteriaModel.setSubmitter(submitter.getValue());
    }

    @UiField
    PromptedTextBox submitter;


    /**
     * Event handler for handling changes in the selected submitter
     * @param event The ValueChangeEvent
     */
    @UiHandler("submitter")
    void filterSelectionChanged(ValueChangeEvent<String> event) {
        jobListCriteriaModel.setSubmitter(submitter.getValue());
    }


    @Override
    public String getName() {
        return texts.submitterFilter_name();
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return submitter.addChangeHandler( changeHandler );
    }

}