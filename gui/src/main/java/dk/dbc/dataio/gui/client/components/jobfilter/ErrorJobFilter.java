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
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;


/**
 * This is the Sink Job Filter
 */
public class ErrorJobFilter extends BaseJobFilter {
    interface SinkJobFilterUiBinder extends UiBinder<HTMLPanel, ErrorJobFilter> {
    }

    private static SinkJobFilterUiBinder ourUiBinder = GWT.create(SinkJobFilterUiBinder.class);

    ChangeHandler callbackChangeHandler = null;


    @UiConstructor
    public ErrorJobFilter() {
        this(GWT.create(Texts.class), GWT.create(Resources.class));
    }

    @Inject
    public ErrorJobFilter(Texts texts, Resources resources) {
        super(texts, resources);
        initWidget(ourUiBinder.createAndBindUi(this));
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

    /*
     * Override HasChangeHandlers Interface Methods
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        callbackChangeHandler = changeHandler;
        callbackChangeHandler.onChange(null);
        return () -> callbackChangeHandler = null;
    }

    @Override
    public JobListCriteria getValue() {
        CriteriaClass criteriaClass = new CriteriaClass();
        criteriaClass.or(processingCheckBox.getValue(), JobListCriteria.Field.STATE_PROCESSING_FAILED);
        criteriaClass.or(deliveringCheckBox.getValue(), JobListCriteria.Field.STATE_DELIVERING_FAILED);
        criteriaClass.or(jobCreationCheckBox.getValue(), JobListCriteria.Field.WITH_FATAL_ERROR);
        return criteriaClass.getCriteria();
    }


    /*
     * Private classes
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
