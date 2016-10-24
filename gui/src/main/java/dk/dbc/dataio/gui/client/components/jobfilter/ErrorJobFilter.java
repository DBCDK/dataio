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
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

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
        this(GWT.create(Texts.class), GWT.create(Resources.class), "");
    }

    @Inject
    public ErrorJobFilter(Texts texts, Resources resources, @Named("Empty") String parameter) {
        super(texts, resources);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameterData(parameter);
    }

    @UiField CheckBox processingCheckBox;
    @UiField CheckBox deliveringCheckBox;
    @UiField CheckBox jobCreationCheckBox;


    /**
     * Event handler for handling changes in the selection of error filtering
     * @param event The ValueChangeEvent
     */
    @UiHandler(value = {"processingCheckBox", "deliveringCheckBox", "jobCreationCheckBox"})
    @SuppressWarnings("unused")
    void checkboxValueChanged(ValueChangeEvent<Boolean> event) {
        // Signal change to caller
        if (callbackChangeHandler != null) {
            callbackChangeHandler.onChange(null);
        }
    }

    /**
     * Fetches the name of this filter
     * @return The name of the filter
     */
    @Override
    public String getName() {
        return texts.errorFilter_name();
    }

    /**
     * Gets the value of the job filter, which is the constructed JobListCriteria for this job filter
     * @return The constructed JobListCriteria for this job filter
     */
    @Override
    public JobListCriteria getValue() {
        CriteriaClass criteriaClass = new CriteriaClass();
        criteriaClass.or(processingCheckBox.getValue(), STATE_PROCESSING_FAILED);
        criteriaClass.or(deliveringCheckBox.getValue(), JobListCriteria.Field.STATE_DELIVERING_FAILED);
        criteriaClass.or(jobCreationCheckBox.getValue(), JobListCriteria.Field.WITH_FATAL_ERROR);
        return criteriaClass.getCriteria();
    }

    /**
     * Sets the selection according to the value, setup in the parameter attribute<br>
     * The value is one (or more) of the texts: Processing, Delivering og JobCreation<br>
     * If more that one of the texts are given, they are separated by commas.<br>
     * The case of the texts is not important
     * @param filterParameter The filter parameters to be used by this job filter
     */
    @Override
    public void setParameterData(String filterParameter) {
        if (!filterParameter.isEmpty()) {
            String[] data = filterParameter.split(",", 3);
            processingCheckBox.setValue(false);
            deliveringCheckBox.setValue(false);
            jobCreationCheckBox.setValue(false);
            for (String item: data) {
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
