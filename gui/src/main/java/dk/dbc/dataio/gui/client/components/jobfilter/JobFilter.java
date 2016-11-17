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
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.pages.job.show.Presenter;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class implements the generic Jobs Filter as a UI Binder component.<br>
 * To be added in the top of the Jobs List.<br>
 * The component contains an "Add Filter" menu and a button to be used to activate the filter:
 * <pre>
 * <code>
 * +---------------+
 * | Tilføj Filter |
 * +---------------+
 * </code>
 * </pre>
 * When the menu "Add Filter" is clicked, a sub menu will appear, containing the names of all available filters.<br>
 * These filters are configured in the {@link JobFilterList} class.<br>
 * In UI Binder, add the following:
 * <pre>
 * <code>
 *  &lt;jobs:JobFilter ui:field="jobFilter"/&gt;
 * </code>
 * </pre>
 */
public class JobFilter extends Composite implements HasChangeHandlers {
    interface JobFilterUiBinder extends UiBinder<HTMLPanel, JobFilter> {

    }
    private static JobFilterUiBinder ourUiBinder = GWT.create(JobFilterUiBinder.class);

    final JobFilterList availableJobFilters;
    ChangeHandler changeHandler = null;
    AbstractBasePlace place = null;
    private boolean filterMenuNotYetInitialized = true;

    @UiField FlowPanel jobFilterPanel;
    @UiField MenuBar filterMenu;


    /**
     * Default empty Constructor
     */
    @SuppressWarnings("unused")
    @UiConstructor
    public JobFilter() {
        this(new JobFilterList());
    }

    /**
     * Constructor with list of Available Job Filters to be shown upon startup
     * @param availableJobFilters The list of Available Job Filters
     */
    JobFilter(JobFilterList availableJobFilters) {
        this.availableJobFilters = availableJobFilters;
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    /**
     * This method is called immediately after the filter becomes attached to the browser's document <br>
     * Go through all filters in the JobFilterList, add them to the menu, and start the ones up, that have been marked for it.<br>
     * The reason why this piece of code is delayed (not called in the constructor) is, that the actual filter calls the place,
     * which has not been initialized in the constructor.
     */
    @Override
    public void onLoad() {
        if (filterMenuNotYetInitialized) {
            filterMenuNotYetInitialized = false;
            availableJobFilters.getJobFilterList().forEach(filter -> {
                filterMenu.addItem(filter.jobFilter.getName(), filter.jobFilter.getAddCommand(this));
                if (place.getParameters().isEmpty() && filter.activeOnStartup) {  // Only use activeOnStartup if no parameters are given in the URL
                    filter.jobFilter.getAddCommand(this).execute();
                }
            });
            Presenter presenter = (Presenter) place.presenter;
            if (presenter != null) {
                presenter.setPlace(place);
            }
        }
    }

    /*
     * HasChangeHandlers Interface Methods
     */

    /**
     * Adds a change handler to be notified upon changes in the stored Job List Criteria Model
     * @param changeHandler The change handler to be notified upon changes
     * @return A Handler Registration object, to be used to remove the Change Handler
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        this.changeHandler = changeHandler;
        return this::removeChangeHandler;
    }


    /*
     * Public Methods
     */

    /**
     * Injects the place into the filter. <br>
     * If set, it allows the filter to maintain the url, while adding and removing filters
     *
     * @param place The place to inject into the filter
     */
    public void setPlace(AbstractBasePlace place) {
        this.place = place;
    }

    /**
     * Adds a child Job Filter to the list of Job Filters <br>
     * These jobs are listed in the Job Filter Menu
     * @param jobFilter The job filter to add to the list of Job Filters
     */
    public void add(BaseJobFilter jobFilter) {
        if (jobFilter != null) {
            jobFilterPanel.add(jobFilter.filterPanel);
            jobFilter.addChangeHandler(changeEvent -> valueChanged());
            valueChanged();  // Do assure, that whenever a filter is being applied, do the filtering
            jobFilter.setFocus(true);
        }
    }

    /**
     * Removes a child Job Filter from the list of Job Filters
     * @param jobFilter The job filter to remove from the list of Job Filters
     */
    public void remove(BaseJobFilter jobFilter) {
        if (jobFilter != null && jobFilter.filterPanel != null) {
            jobFilterPanel.remove(jobFilter.filterPanel);
            jobFilter.filterPanel = null;
            valueChanged();  // Do assure, that whenever a filter is being removed, do the filtering
        }
    }

    /**
     * Gets the current value of the Job List Criteria Model
     * @return The current value of the Job List Criteria Model
     */
    public JobListCriteria getValue() {
        JobListCriteria jobListCriteria = new JobListCriteria();

        // Now do find all derivatives of the BaseJobFilter - eg SinkJobFilter, and get it's JobListCriteriaModel
        for (Widget decoratorPanelWidget : jobFilterPanel) {
            if (decoratorPanelWidget instanceof JobFilterPanel) {
                JobFilterPanel jobFilterPanel = (JobFilterPanel) decoratorPanelWidget;
                Iterator<Widget> baseJobFilterIterator = jobFilterPanel.iterator();  // Inner level: Find BaseJobFilter's - or any derivative
                if (baseJobFilterIterator.hasNext()) {
                    Widget baseJobFilterWidget = baseJobFilterIterator.next();
                    if (baseJobFilterWidget instanceof BaseJobFilter) {
                        BaseJobFilter baseJobFilter = (BaseJobFilter) baseJobFilterWidget;
                        JobListCriteria model = baseJobFilter.getValue();
                        jobListCriteria.and(model);
                    }
                }
            }
        }
        return jobListCriteria;
    }

    /**
     * Setup parameters for all filters<br>
     * Each filter is identified by the key to the map - a string with the ClassName of the Job Filter (getSimpleName())
     *
     * @param parameters A map containing the setup parameters for the job filters
     * @return A map of parameters, that was recognized and set by the filter
     */
    public Map<String, String> setupFilterParameters(Map<String, String> parameters) {
        Map<String, String> recognizedParameters = new LinkedHashMap<>();
        if (!parameters.isEmpty()) {
            availableJobFilters.getJobFilterList().forEach(filter -> filter.jobFilter.removeJobFilter(false));
            new LinkedHashMap<>(parameters).forEach((filterName, filterParameter) -> {  // Use a clone of parameters to avoid ConcurrentModificationException
                String foundFilterName = JobFilter.this.setupFilterParameterFor(filterName, filterParameter);
                if (foundFilterName != null) {
                    recognizedParameters.put(foundFilterName, filterParameter);
                }
            });
        }
        return recognizedParameters;
    }


    /*
     * Private methods and classes
     */

    private void removeChangeHandler() {
        changeHandler = null;
    }

    private class JobFilterChangeEvent extends ChangeEvent {}

    private void valueChanged() {
        if (changeHandler != null) {
            changeHandler.onChange(new JobFilterChangeEvent());
        }
    }

    /**
     * Setup filter parameters for the filter, whose name is given as a parameter
     * @param filterName The filter to setup
     * @param filterParameter The parameter to send to the filter
     * @return If a match is found, then the filter name of that filter is returned, if no math is found - null is returned
     */
    private String setupFilterParameterFor(String filterName, String filterParameter) {
        for (JobFilterList.JobFilterItem filter: availableJobFilters.getJobFilterList()) {
            if (filter.jobFilter.getClass().getSimpleName().toLowerCase().equals(filterName.toLowerCase())) {
                filter.jobFilter.setParameter(filterParameter);
                filter.jobFilter.addJobFilter();
                return filterName;  // This is the found match
            }
        }
        return null;
    }

}