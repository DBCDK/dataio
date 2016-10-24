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
import com.google.inject.Inject;
import com.google.inject.name.Named;
import dk.dbc.dataio.gui.client.components.PromptedDateTimeBox;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.Date;

/**
 * This is the Date Job Filter
 */
public class DateJobFilter extends BaseJobFilter {
    private final static Integer TWO_DAYS_IN_MILLISECONDS = 2*24*60*60*1000;
    private final static String DEFAULT_TO_DATE = "";
    private final static String DEFAULT_EMPTY_TIME = "00:00:00";

    interface DateJobFilterUiBinder extends UiBinder<HTMLPanel, DateJobFilter> {
    }

    private static DateJobFilterUiBinder ourUiBinder = GWT.create(DateJobFilterUiBinder.class);

    ChangeHandler callbackChangeHandler = null;


    @UiField PromptedDateTimeBox fromDate;
    @UiField PromptedDateTimeBox toDate;


    @SuppressWarnings("unused")
    @UiConstructor
    public DateJobFilter() {
        this(GWT.create(Texts.class), GWT.create(Resources.class), "");
    }

    @Inject
    public DateJobFilter(Texts texts, Resources resources, @Named("Empty") String parameter) {
        super(texts, resources);
        initWidget(ourUiBinder.createAndBindUi(this));
        fromDate.setValue(defaultFromDate());
        toDate.setValue(DEFAULT_TO_DATE);
        setParameterData(parameter);
    }

    /**
     * Event handler for handling changes in the selection of from and to dates
     * @param event The ValueChangeEvent
     */
    @SuppressWarnings("unused")
    @UiHandler(value={"fromDate", "toDate"})
    void dateChanged(ValueChangeEvent<String> event) {
        // Signal change to caller
        if (callbackChangeHandler != null) {
            callbackChangeHandler.onChange(null);
        }
    }


     /*
     * Abstract methods from BaseJobFilter
     */

    /**
     * Fetches the name of this filter
     * @return The name of the filter
     */
    @Override
    public String getName() {
        return texts.jobDateFilter_name();
    }

    /**
     * Gets the value of the job filter, which is the constructed JobListCriteria to be used in the filter search
     * @return The constructed JobListCriteria filter
     */
    @Override
    public JobListCriteria getValue() {
        JobListCriteria criteria = new JobListCriteria();
        if (!fromDate.getValue().isEmpty()) {
            criteria = criteria.and(new JobListCriteria().where(
                    new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION,
                            ListFilter.Op.GREATER_THAN,
                            Format.parseLongDateAsDate(fromDate.getValue()))));
        }
        if (!toDate.getValue().isEmpty()) {
            criteria = criteria.and(new JobListCriteria().where(
                    new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION,
                            ListFilter.Op.LESS_THAN,
                            Format.parseLongDateAsDate(toDate.getValue()))));
        }
        return criteria;
    }

    /**
     * Sets the selection according to the value, setup in the parameter attribute<br>
     * The value is one or two dates, separated by commas
     * @param filterParameter The filter parameters to be used by this job filter
     */
    @Override
    public void setParameterData(String filterParameter) {
        if (!filterParameter.isEmpty()) {
            String[] data = filterParameter.split(",", 2);
            fromDate.setValue(data[0], true);
            if (data.length == 2) {
                toDate.setValue(data[1], true);
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

    /**
     * Calculates the default "from" date to be used. Should be set to the current day minus two days, at 00:00:00
     * @return The default From date
     */
    private String defaultFromDate() {
        String date = Format.formatLongDate(new Date(System.currentTimeMillis()-TWO_DAYS_IN_MILLISECONDS));
        return date.substring(0, date.length() - DEFAULT_EMPTY_TIME.length()) + DEFAULT_EMPTY_TIME;
    }

}