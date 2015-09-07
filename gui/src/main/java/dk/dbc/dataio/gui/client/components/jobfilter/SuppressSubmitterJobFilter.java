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
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

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
        if (suppressSubmittersButton.getValue()) {
            String jsonMatch= "{ \"submitterId\": 870970}";
            return new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_NOT_LEFT_CONTAINS, jsonMatch));
        } else {
            return new JobListCriteria();  // No submitters suppressed
        }
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        callbackChangeHandler = changeHandler;
        callbackChangeHandler.onChange(null);
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                callbackChangeHandler = null;
            }
        };
    }

}