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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This class implements the generic Jobs Filter as a UI Binder component.<br>
 * To be added in the top of the Jobs List.<br>
 * The component contains an "Add Filter" menu and a deleteButton to be used to activate the filter:
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

    final JobFilterList availableJobFilterList;
    ChangeHandler changeHandler = null;
    AbstractBasePlace place = null;
    boolean initialized = false;
    final Map<String, BaseJobFilter> instantiatedFilters = new HashMap<>();  // Keeps track of all instantiated filters - whether or not they are attached to the GUI

    @UiField
    FlowPanel jobFilterContainer;
    @UiField
    MenuBar filterMenu;


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
     *
     * @param availableJobFilterList The list of Available Job Filters
     */
    JobFilter(JobFilterList availableJobFilterList) {
        this.availableJobFilterList = availableJobFilterList;
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
        onLoad(place.getClass().getSimpleName());
    }

    /**
     * This method is the body of the empty-parameter onLoad method<br>
     * the getClass() method of an object cannot be mocked - therefore this method
     * allows unit test of the onLoad method by injecting the place name
     *
     * @param placeName The place name used to select the correct filter list
     */
    void onLoad(String placeName) {
        Map<String, AbstractBasePlace.PlaceParameterValue> urlParameters = place.getDetailedParameters();
        List<JobFilterList.JobFilterItem> availableFilters = availableJobFilterList.getJobFilters(placeName);

        if (availableFilters != null) {
            // First create the menu with the available Job Filters
            if (!initialized) {
                availableFilters.forEach(filter -> {
                    instantiatedFilters.put(filter.jobFilter.getClass().getSimpleName(), filter.jobFilter);
                    filterMenu.addItem(filter.jobFilter.getName(), filter.jobFilter.getAddCommand(this));
                });
            }

            // Then create the active Job Filters, given by either the URL or Default Job Filters (set in JobFilterList)
            if (!initialized) {
                if (urlParameters.isEmpty()) {  // If upon startup, no URL parameters are given, do search in the JobFilterList for default filters
                    availableFilters.forEach(filter -> {
                        if (filter.activeOnStartup) {
                            filter.jobFilter.getAddCommand(this).execute();
                        }
                    });
                } else {  // If upon startup, there are some URL parameters present, start these filters
                    setNewUrlParameters(urlParameters);
                }
            } else {  // Filters have been initialized
                if (!urlParameters.isEmpty()) {  // Only set URL parameters if present - if not, just use filters as they are
                    setNewUrlParameters(urlParameters);
                }
            }
        }

        // Finally do replicate setting of the active Job Filters to the Place
        if (place.presenter != null) {
            ((Presenter) place.presenter).setPlace(place);
        }

        initialized = true;
    }

    /*
     * HasChangeHandlers Interface Methods
     */

    /**
     * Adds a change handler to be notified upon changes in the stored Job List Criteria Model
     *
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
     *
     * @param jobFilter The job filter to add to the list of Job Filters
     */
    public void add(BaseJobFilter jobFilter) {

        if (jobFilter != null) {
            jobFilterContainer.add(jobFilter.filterPanel);
            jobFilter.addChangeHandler(changeEvent -> valueChanged());
            valueChanged();  // Do assure, that whenever a filter is being applied, do the filtering
            jobFilter.setFocus(true);
        }
    }

    /**
     * Removes a child Job Filter from the list of Job Filters
     *
     * @param jobFilter The job filter to remove from the list of Job Filters
     */
    public void remove(BaseJobFilter jobFilter) {
        if (jobFilter != null && jobFilter.filterPanel != null) {
            jobFilterContainer.remove(jobFilter.filterPanel);
            jobFilter.parentJobFilter.place.removeParameter(jobFilter.getParameterKeyName());
            jobFilter.initialInvertFilterValue = jobFilter.filterPanel.isInvertFilter();  // Do assure, that filterPanel will be instantiated with the same invert state as now
            jobFilter.filterPanel = null;
            valueChanged();  // Do assure, that whenever a filter is being removed, do the filtering
        }
    }

    /**
     * Gets the current value of the Job List Criteria Model
     *
     * @return The current value of the Job List Criteria Model
     */
    public JobListCriteria getValue() {
        JobListCriteria jobListCriteria = new JobListCriteria();
        traverseActiveFilters(filter -> jobListCriteria.and(filter.isInvertFilter() ? filter.getValue().not() : filter.getValue()));
        return jobListCriteria;
    }

    /**
     * Update the place to reflect the current status of the filter settings
     *
     * @param place The place to update
     */
    public void updatePlace(AbstractBasePlace place) {
        traverseActiveFilters(filter -> place.addParameter(filter.getParameterKeyName(), filter.isInvertFilter(), filter.getParameter()));
    }



    /*
     * Private methods and classes
     */

    private void removeChangeHandler() {
        changeHandler = null;
    }

    private class JobFilterChangeEvent extends ChangeEvent {
    }

    private void valueChanged() {
        if (changeHandler != null) {
            changeHandler.onChange(new JobFilterChangeEvent());
        }
    }

    /**
     * Traverses through the list of active filters (filters that are actively used), and calls
     * the functional interface Consumer on each element.
     *
     * @param action The functional interface to call, for each found active filter
     */
    private void traverseActiveFilters(Consumer<BaseJobFilter> action) {
        for (Widget widget : jobFilterContainer) {
            if (widget instanceof JobFilterPanel) {
                JobFilterPanel jobFilterPanel = (JobFilterPanel) widget;
                Iterator<Widget> baseJobFilterIterator = jobFilterPanel.iterator();  // Inner level: Find BaseJobFilter's - or any derivative
                if (baseJobFilterIterator.hasNext()) {
                    Widget baseJobFilterWidget = baseJobFilterIterator.next();
                    if (baseJobFilterWidget instanceof BaseJobFilter) {
                        action.accept((BaseJobFilter) baseJobFilterWidget);
                    }
                }
            }
        }
    }

    /**
     * Sets up the Job Filters according to the supplied list of URL Parameters<br>
     * Also removes any Job Filters, not given in the supplied list
     *
     * @param urlParameters The URL parameters used as input
     */
    private void setNewUrlParameters(Map<String, AbstractBasePlace.PlaceParameterValue> urlParameters) {
        // First remove (de-attach) all filters, that aren't attached and not mentioned in the URL parameter list
        instantiatedFilters.forEach((name, filter) -> {
            if (!urlParameters.containsKey(name)) {
                filter.removeJobFilter(false);
            }
        });
        // Then do attach (add) all filters in the URL parameter list and setup parameters
        urlParameters.forEach((name, parameter) -> {
            if (instantiatedFilters.containsKey(name)) {
                BaseJobFilter filter = instantiatedFilters.get(name);
                filter.instantiateJobFilter(false);
                filter.setParameter(parameter.isInvert(), parameter.getValue());
            }
        });
    }

}
