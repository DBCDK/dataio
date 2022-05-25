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
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

/**
 * This is the Submitter Job Filter
 */
public class SubmitterJobFilter extends BaseJobFilter {
    interface SubmitterJobFilterUiBinder extends UiBinder<HTMLPanel, SubmitterJobFilter> {
    }

    private static SubmitterJobFilterUiBinder ourUiBinder = GWT.create(SubmitterJobFilterUiBinder.class);

    @SuppressWarnings("unused")
    @UiConstructor
    public SubmitterJobFilter() {
        this("", false);
    }

    SubmitterJobFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    SubmitterJobFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }

    @UiField
    PromptedTextBox submitter;


    /**
     * Event handler for handling changes in the submitter value
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("submitter")
    @SuppressWarnings("unused")
    void submitterValueChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    /**
     * Gets the name of the job filter
     *
     * @return the name of the job filter
     */
    @Override
    public String getName() {
        return texts.submitterFilter_name();
    }

    /**
     * Gets the current value of the job filter
     *
     * @return the current value of the filter
     */
    @Override
    public JobListCriteria getValue() {
        String enteredValue = submitter.getValue();
        if (enteredValue == null || enteredValue.isEmpty()) return new JobListCriteria();

        final String[] values = enteredValue.split("\\s*,\\s*");  // Entered value might contain a comma separated list of submitters

        JobListCriteria jobListCriteria = new JobListCriteria();
        boolean first = true;
        for (String value : values) {
            ListFilter listFilter = new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"submitterId\": " + value + "}");
            if (first) {
                jobListCriteria = jobListCriteria.where(listFilter);
            } else {
                jobListCriteria = jobListCriteria.or(listFilter);
            }
            first = false;
        }
        return jobListCriteria;

    }

    /**
     * Sets the selection according to the key value, setup in the parameter attribute<br>
     * The value is given in url as a plain integer, as the submitter number
     *
     * @param filterParameter The filter parameters to be used by this job filter
     */
    @Override
    public void localSetParameter(String filterParameter) {
        if (!filterParameter.isEmpty()) {
            submitter.setValue(filterParameter, true);
        }
    }

    /**
     * Gets the parameter value for the filter
     *
     * @return The stored filter parameter for the specific job filter
     */
    @Override
    public String getParameter() {
        return submitter.getValue();
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus at a time, and the widget that does will receive all keyboard events.
     *
     * @param focused whether this widget should take focus or release it
     */
    @Override
    public void setFocus(boolean focused) {
        submitter.setFocus(focused);
    }


    /**
     * Adds a changehandler to the job filter
     *
     * @param changeHandler the changehandler
     * @return a Handler Registration object
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return submitter.addChangeHandler(changeHandler);
    }


}
