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
import com.google.gwt.user.client.ui.RadioButton;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;


/**
 * This is the Job Status Filter
 */
public class JobStatusFilter extends BaseJobFilter {
    private final String ACTIVE_TEXT = "active";
    private final String PREVIEW_TEXT = "preview";
    private final String DONE_TEXT = "done";
    private final String FAILED_TEXT = "failed";

    interface JobStatusFilterUiBinder extends UiBinder<HTMLPanel, JobStatusFilter> {
    }

    private static JobStatusFilterUiBinder ourUiBinder = GWT.create(JobStatusFilterUiBinder.class);

    private ChangeHandler callbackChangeHandler = null;

    @SuppressWarnings("unused")
    @UiConstructor
    public JobStatusFilter() {
        this("", false);
    }

    JobStatusFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    JobStatusFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }


    @UiField
    RadioButton activeRadioButton;
    @UiField
    RadioButton previewRadioButton;
    @UiField
    RadioButton doneRadioButton;
    @UiField
    RadioButton failedRadioButton;


    /**
     * Event handler for handling changes in the selection of error filtering
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler(value = {"activeRadioButton", "previewRadioButton", "doneRadioButton", "failedRadioButton"})
    @SuppressWarnings("unused")
    void RadioButtonValueChanged(ValueChangeEvent<Boolean> event) {
        filterChanged();
        if (callbackChangeHandler != null) {
            callbackChangeHandler.onChange(null);
        }
    }

    /**
     * Gets the  name of the filter
     *
     * @return The name of the filter
     */
    @Override
    public String getName() {
        return texts.jobStatusFilter_name();
    }

    /**
     * Gets the JobListCriteria constructed by this job filter
     *
     * @return The JobListCriteria constructed by this job filter
     */
    @Override
    public JobListCriteria getValue() {
        JobListCriteria jobListCriteria = new JobListCriteria();
        if (activeRadioButton.getValue()) {
            jobListCriteria.where(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL));
        } else if (previewRadioButton.getValue()) {
            jobListCriteria.where(new ListFilter<>(JobListCriteria.Field.PREVIEW_ONLY));
        } else if (doneRadioButton.getValue()) {
            jobListCriteria.where(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NOT_NULL));
        } else {
            jobListCriteria.where(new ListFilter<>(JobListCriteria.Field.JOB_CREATION_FAILED))
                    .or(new ListFilter<>(JobListCriteria.Field.STATE_PROCESSING_FAILED))
                    .or(new ListFilter<>(JobListCriteria.Field.STATE_DELIVERING_FAILED));
        }
        return jobListCriteria;
    }

    /**
     * Sets the selection according to the value, setup in the parameter attribute<br>
     * The value is one of the texts: Active, Waiting, Done and Failed<br>
     * Example:  'Done'  <br>
     * The case of the texts is not important
     *
     * @param filterParameter The filter parameters to be used by this job filter
     */
    @Override
    public void localSetParameter(String filterParameter) {
        if (!filterParameter.isEmpty()) {
            activeRadioButton.setValue(false);
            previewRadioButton.setValue(false);
            doneRadioButton.setValue(false);
            failedRadioButton.setValue(false);
            switch (filterParameter.toLowerCase()) {
                case ACTIVE_TEXT:
                    activeRadioButton.setValue(true);
                    break;
                case PREVIEW_TEXT:
                    previewRadioButton.setValue(true);
                    break;
                case DONE_TEXT:
                    doneRadioButton.setValue(true);
                    break;
                case FAILED_TEXT:
                    failedRadioButton.setValue(true);
                    break;
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
        if (activeRadioButton.getValue()) {
            return ACTIVE_TEXT;
        } else if (previewRadioButton.getValue()) {
            return PREVIEW_TEXT;
        } else if (doneRadioButton.getValue()) {
            return DONE_TEXT;
        } else if (failedRadioButton.getValue()) {
            return FAILED_TEXT;
        } else {
            return "";
        }
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

}
