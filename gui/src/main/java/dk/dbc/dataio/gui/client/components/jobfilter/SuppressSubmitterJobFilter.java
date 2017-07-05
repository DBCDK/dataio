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
import com.google.gwt.event.dom.client.ClickEvent;
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
 * This is the Submitter Job Filter
 */
public class SuppressSubmitterJobFilter extends BaseJobFilter {
    interface SubmitterJobFilterUiBinder extends UiBinder<HTMLPanel, SuppressSubmitterJobFilter> {
    }

    private static SubmitterJobFilterUiBinder ourUiBinder = GWT.create(SubmitterJobFilterUiBinder.class);

    @SuppressWarnings("unused")
    @UiConstructor
    public SuppressSubmitterJobFilter() {
        this("", true);
    }

    SuppressSubmitterJobFilter(String parameter, boolean includeFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, includeFilter);
    }

    SuppressSubmitterJobFilter(Texts texts, Resources resources, String parameter, boolean includeFilter) {
        super(texts, resources, includeFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }

    ChangeHandler callbackChangeHandler = null;

    @UiField RadioButton showAllSubmittersButton;
    @UiField RadioButton suppressSubmittersButton;



    /**
     * Event handler for handling changes in the suppressed submitter
     * @param event The ValueChangeEvent
     */
    @SuppressWarnings("unused")
    @UiHandler(value={"showAllSubmittersButton", "suppressSubmittersButton"})
    void filterItemsRadioButtonPressed(ClickEvent event) {
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
        return texts.suppressSubmitterFilter_name();
    }

    /**
     * Gets the JobListCriteria constructed by this job filter
     * @return The JobListCriteria constructed by this job filter
     */
    @Override
    public JobListCriteria getValue() {
        if (suppressSubmittersButton.getValue()) {
            String jsonMatch= "{ \"submitterId\": 870970}";
            return new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_NOT_LEFT_CONTAINS, jsonMatch));
        } else {
            return new JobListCriteria();  // No submitters suppressed
        }
    }

    /**
     * Sets the selection according to the key value, setup in the parameter attribute<br>
     * If the value given in url is a not-empty string, this filter is set to active
     * @param filterParameter The filter parameters to be used by this job filter
     */
    @Override
    public void setParameter(String filterParameter) {
        showAllSubmittersButton.setValue(!filterParameter.isEmpty(), true);
        suppressSubmittersButton.setValue(filterParameter.isEmpty(), true);
    }

    /**
     * Gets the parameter value for the filter
     * @return The stored filter parameter for the specific job filter
     */
    @Override
    public String getParameter() {
        return showAllSubmittersButton.getValue() ? "disable" : "";
    }

    /**
     * Adds a ChangeHandler for this job filter
     * @param changeHandler The ChangeHandler for this job filter
     * @return A HandlerRegistration object to be used to remove the job filter
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        callbackChangeHandler = changeHandler;
        callbackChangeHandler.onChange(null);
        return () -> callbackChangeHandler = null;
    }


}