/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    private final String WAITING_TEXT = "waiting";
    private final String DONE_TEXT = "done";
    private final String FAILED_TEXT = "failed";

    interface JobStatusFilterUiBinder extends UiBinder<HTMLPanel, JobStatusFilter> {
    }

    private static JobStatusFilterUiBinder ourUiBinder = GWT.create(JobStatusFilterUiBinder.class);

    private ChangeHandler callbackChangeHandler = null;

    @SuppressWarnings("unused")
    @UiConstructor
    public JobStatusFilter() {
        this("");
    }

    JobStatusFilter(String parameter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter);
    }

    JobStatusFilter(Texts texts, Resources resources, String parameter) {
        super(texts, resources);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }


    @UiField RadioButton activeRadioButton;
    @UiField RadioButton waitingRadioButton;
    @UiField RadioButton doneRadioButton;
    @UiField RadioButton failedRadioButton;


    /**
     * Event handler for handling changes in the selection of error filtering
     * @param event The ValueChangeEvent
     */
    @UiHandler(value = {"activeRadioButton", "waitingRadioButton", "doneRadioButton", "failedRadioButton"})
    @SuppressWarnings("unused")
    void RadioButtonValueChanged(ValueChangeEvent<Boolean> event) {
        filterChanged();
        if (callbackChangeHandler != null) {
            callbackChangeHandler.onChange(null);
        }
    }

    /**
     * Gets the  name of the filter
     * @return The name of the filter
     */
    @Override
    public String getName() {
        return texts.jobStatusFilter_name();
    }

    /**
     * Gets the JobListCriteria constructed by this job filter
     * @return The JobListCriteria constructed by this job filter
     */
    @Override
    public JobListCriteria getValue() {
        // To be implemented (example from ErrorJobFilter):
//        JobStatusFilter.CriteriaClass criteriaClass = new JobStatusFilter.CriteriaClass();
//        criteriaClass.or(processingCheckBox.getValue(), STATE_PROCESSING_FAILED);
//        criteriaClass.or(deliveringCheckBox.getValue(), JobListCriteria.Field.STATE_DELIVERING_FAILED);
//        criteriaClass.or(jobCreationCheckBox.getValue(), JobListCriteria.Field.JOB_CREATION_FAILED);
//        return criteriaClass.getCriteria();
        if (activeRadioButton.getValue()) {
            return new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL));
        } else {
            return new JobListCriteria();
        }
    }

    /**
     * Sets the selection according to the value, setup in the parameter attribute<br>
     * The value is one of the texts: Active, Waiting, Done and Failed<br>
     * Example:  'Done'  <br>
     * The case of the texts is not important
     * @param filterParameter The filter parameters to be used by this job filter
     */
    @Override
    public void setParameter(String filterParameter) {
        if (!filterParameter.isEmpty()) {
            activeRadioButton.setValue(false);
            waitingRadioButton.setValue(false);
            doneRadioButton.setValue(false);
            failedRadioButton.setValue(false);
            switch (filterParameter.toLowerCase()) {
                case ACTIVE_TEXT:
                    activeRadioButton.setValue(true);
                    break;
                case WAITING_TEXT:
                    waitingRadioButton.setValue(true);
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
     * @return The stored filter parameter for the specific job filter
     */
    @Override
    public String getParameter() {
        if (activeRadioButton.getValue()) {
            return ACTIVE_TEXT;
        } else if (waitingRadioButton.getValue()) {
            return WAITING_TEXT;
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