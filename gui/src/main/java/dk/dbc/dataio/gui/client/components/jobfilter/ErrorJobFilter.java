package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.jobstore.types.criteria.JobListCriteria.Field.STATE_PROCESSING_FAILED;


/**
 * This is the Sink Job Filter
 */
public class ErrorJobFilter extends BaseJobFilter {
    private final String PROCESSING_TEXT = "processing";
    private final String DELIVERING_TEXT = "delivering";
    private final String JOB_CREATION_TEXT = "jobcreation";

    interface SinkJobFilterUiBinder extends UiBinder<HTMLPanel, ErrorJobFilter> {
    }

    private static SinkJobFilterUiBinder ourUiBinder = GWT.create(SinkJobFilterUiBinder.class);

    ChangeHandler callbackChangeHandler = null;


    @SuppressWarnings("unused")
    @UiConstructor
    public ErrorJobFilter() {
        this("", false);
    }

    ErrorJobFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    ErrorJobFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }

    @UiField
    CheckBox processingCheckBox;
    @UiField
    CheckBox deliveringCheckBox;
    @UiField
    CheckBox jobCreationCheckBox;


    /**
     * Event handler for handling changes in the selection of error filtering
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler(value = {"processingCheckBox", "deliveringCheckBox", "jobCreationCheckBox"})
    @SuppressWarnings("unused")
    void checkboxValueChanged(ValueChangeEvent<Boolean> event) {
        filterChanged();
        if (callbackChangeHandler != null) {
            callbackChangeHandler.onChange(null);
        }
    }

    /**
     * Fetches the name of this filter
     *
     * @return The name of the filter
     */
    @Override
    public String getName() {
        return texts.errorFilter_name();
    }

    /**
     * Gets the value of the job filter, which is the constructed JobListCriteria for this job filter
     *
     * @return The constructed JobListCriteria for this job filter
     */
    @Override
    public JobListCriteria getValue() {
        CriteriaClass criteriaClass = new CriteriaClass();
        criteriaClass.or(processingCheckBox.getValue(), STATE_PROCESSING_FAILED);
        criteriaClass.or(deliveringCheckBox.getValue(), JobListCriteria.Field.STATE_DELIVERING_FAILED);
        criteriaClass.or(jobCreationCheckBox.getValue(), JobListCriteria.Field.JOB_CREATION_FAILED);
        return criteriaClass.getCriteria();
    }

    /**
     * Sets the selection according to the value, setup in the parameter attribute<br>
     * The value is one (or more) of the texts: Processing, Delivering og JobCreation<br>
     * If more that one of the texts are given, they are separated by commas.<br>
     * Example:  'Processing,jobcreation'  <br>
     * The case of the texts is not important
     *
     * @param filterParameter The filter parameters to be used by this job filter
     */
    @Override
    public void localSetParameter(String filterParameter) {
        if (!filterParameter.isEmpty()) {
            String[] data = filterParameter.split(",", 3);
            processingCheckBox.setValue(false);
            deliveringCheckBox.setValue(false);
            jobCreationCheckBox.setValue(false);
            for (String item : data) {
                switch (item.toLowerCase()) {
                    case PROCESSING_TEXT:
                        processingCheckBox.setValue(true);
                        break;
                    case DELIVERING_TEXT:
                        deliveringCheckBox.setValue(true);
                        break;
                    case JOB_CREATION_TEXT:
                        jobCreationCheckBox.setValue(true);
                        break;
                }
            }
        }
    }

    /**
     * Gets the parameter value for the filter
     *
     * @return The stored filter parameter for the specific job filter
     */
    @Override
    public String getParameter() {
        List<String> errors = new ArrayList<>();
        if (processingCheckBox.getValue()) {
            errors.add(PROCESSING_TEXT);
        }
        if (deliveringCheckBox.getValue()) {
            errors.add(DELIVERING_TEXT);
        }
        if (jobCreationCheckBox.getValue()) {
            errors.add(JOB_CREATION_TEXT);
        }
        return String.join(",", errors);
    }

    /*
     * Override HasChangeHandlers Interface Methods
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        callbackChangeHandler = changeHandler;
        callbackChangeHandler.onChange(null);
        return () -> callbackChangeHandler = null;
    }


    /*
     * Private
     */

    private class CriteriaClass {
        boolean firstCriteria = true;
        private JobListCriteria criteria = new JobListCriteria();

        public void or(boolean active, JobListCriteria.Field state) {
            if (active) {
                if (firstCriteria) {
                    firstCriteria = false;
                    criteria.where(new ListFilter<>(state));
                } else {
                    criteria.or(new ListFilter<>(state));
                }
            }
        }

        public JobListCriteria getCriteria() {
            return criteria;
        }
    }

}
