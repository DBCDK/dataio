package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.inject.Inject;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

/**
 * This is the Submitter Job Filter
 */
public class SuppressSubmitterJobFilter extends BaseJobFilter {
    interface SubmitterJobFilterUiBinder extends UiBinder<HTMLPanel, SuppressSubmitterJobFilter> {
    }

    private static SubmitterJobFilterUiBinder ourUiBinder = GWT.create(SubmitterJobFilterUiBinder.class);

    @UiConstructor
    public SuppressSubmitterJobFilter() {
        this((Texts) GWT.create(Texts.class), (Resources) GWT.create(Resources.class) );
    }

    @Inject
    public SuppressSubmitterJobFilter(Texts texts, Resources resources) {
        super(texts, resources);
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    ChangeHandler callbackChangeHandler = null;

    @UiField RadioButton showAllSubmittersButton;
    @UiField RadioButton suppressSubmittersButton;



    /**
     * Event handler for handling changes in the suppressed submitter
     * @param event The ValueChangeEvent
     */
    @UiHandler(value={"showAllSubmittersButton", "suppressSubmittersButton"})
    void filterItemsRadioButtonPressed(ClickEvent event) {
        // Set value in JobListCriteria
        if (showAllSubmittersButton.getValue()) {
            GWT.log("SuppressSubmitterJobFilter - Show All Submitters");
//            jobListCriteriaModel.???
        } else if (suppressSubmittersButton.getValue()) {
            GWT.log("SuppressSubmitterJobFilter - Suppress 870970 Submitters");
//            jobListCriteriaModel.???
        }
        // Signal change to caller
        if (callbackChangeHandler != null) {
            callbackChangeHandler.onChange(null);
        }
    }

    @Override
    public String getName() {
        return texts.suppressSubmitterFilter_name();
    }

    @Override
    public JobListCriteria getValue() {
        return new JobListCriteria();
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        callbackChangeHandler = changeHandler;
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                callbackChangeHandler = null;
            }
        };
    }



}