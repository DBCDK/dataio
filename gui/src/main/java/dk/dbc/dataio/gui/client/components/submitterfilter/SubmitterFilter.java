package dk.dbc.dataio.gui.client.components.submitterfilter;

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
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This class implements the generic submitter filter as a UI Binder component.<br>
 * To be added in the top of the submitters list.<br>
 * The component contains an "Add Filter" menu and a deleteButton to be used to activate the filter:
 * <pre>
 * <code>
 * +---------------+
 * | Tilføj Filter |
 * +---------------+
 * </code>
 * </pre>
 * When the menu "Add Filter" is clicked, a sub menu will appear, containing the names of all available filters.<br>
 * These filters are configured in the {@link SubmitterFilterList} class.<br>
 * In UI Binder, add the following:
 * <pre>
 * <code>
 *  &lt;filter:SubmitterFilter ui:field="submitterFilter"/&gt;
 * </code>
 * </pre>
 */
public class SubmitterFilter extends Composite implements HasChangeHandlers {

    interface SubmitterFilterUiBinder extends UiBinder<HTMLPanel, SubmitterFilter> {
    }

    private static SubmitterFilterUiBinder ourUiBinder = GWT.create(SubmitterFilterUiBinder.class);

    final SubmitterFilterList availableFilterList;
    ChangeHandler changeHandler = null;
    AbstractBasePlace place = null;
    boolean initialized = false;
    final Map<String, BaseSubmitterFilter> instantiatedFilters = new HashMap<>();  // Keeps track of all instantiated filters - whether or not they are attached to the GUI

    @UiField
    FlowPanel submitterFilterContainer;
    @UiField
    MenuBar filterMenu;

    @SuppressWarnings("unused")
    @UiConstructor
    public SubmitterFilter() {
        this(new SubmitterFilterList());
    }

    /**
     * Constructor with list of available filters to be shown upon startup
     *
     * @param availableFilterList list of available filters
     */
    SubmitterFilter(SubmitterFilterList availableFilterList) {
        this.availableFilterList = availableFilterList;
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    /**
     * This method is called immediately after the filter becomes
     * attached to the browser's document <br>
     * Goes through all filters in the SubmitterFilterList, adds them to the menu,
     * and starts the ones that have been marked for it.<br>
     * The reason why this piece of code is delayed (not called in the constructor) is,
     * that the actual filter calls the place, which has not been initialized in the constructor.
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
        List<SubmitterFilterList.SubmitterFilterItem> availableFilters = availableFilterList.getFilters(placeName);

        if (availableFilters != null) {
            // First create the menu with the available filters
            if (!initialized) {
                availableFilters.forEach(filter -> {
                    instantiatedFilters.put(filter.submitterFilter.getClass().getSimpleName(), filter.submitterFilter);
                    filterMenu.addItem(filter.submitterFilter.getName(), filter.submitterFilter.getAddCommand(this));
                });
            }

            // Then create the active filters, given by either the URL or default filters (set in SubmitterFilterList)
            if (!initialized) {
                if (urlParameters.isEmpty()) {  // If upon startup, no URL parameters are given, do search in the SubmitterFilterList for default filters
                    availableFilters.forEach(filter -> {
                        if (filter.activeOnStartup) {
                            filter.submitterFilter.getAddCommand(this).execute();
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

        // Finally do replicate setting of the active filters to the Place
        if (place.presenter != null) {
            ((Presenter) place.presenter).setPlace(place);
        }

        initialized = true;
    }

    /**
     * Adds a change handler to be notified upon changes in the stored query clause
     *
     * @param changeHandler change handler to be notified upon changes
     * @return handler registration object, to be used to remove the ChangeHandler
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        this.changeHandler = changeHandler;
        return this::removeChangeHandler;
    }

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
     * Adds a child filter to the list of filters <br>
     *
     * @param submitterFilter filter to add to the list of filters
     */
    public void add(BaseSubmitterFilter submitterFilter) {
        if (submitterFilter != null) {
            submitterFilterContainer.add(submitterFilter.filterPanel);
            submitterFilter.addChangeHandler(changeEvent -> valueChanged());
            valueChanged();  // Do assure, that whenever a filter is being applied, do the filtering
            submitterFilter.setFocus(true);
        }
    }

    /**
     * Removes a child filter from the list of filters
     *
     * @param submitterFilter filter to remove from the list of filters
     */
    public void remove(BaseSubmitterFilter submitterFilter) {
        if (submitterFilter != null && submitterFilter.filterPanel != null) {
            submitterFilterContainer.remove(submitterFilter.filterPanel);
            submitterFilter.parentFilter.place.removeParameter(submitterFilter.getParameterKey());
            submitterFilter.initialInvertFilterValue = submitterFilter.filterPanel.isInvertFilter();  // Do assure, that filterPanel will be instantiated with the same invert state as now
            submitterFilter.filterPanel = null;
            valueChanged();  // Do assure, that whenever a filter is being removed, do the filtering
        }
    }

    /**
     * @return current list of query clauses
     */
    public List<GwtQueryClause> getValue() {
        final ArrayList<GwtQueryClause> clauses = new ArrayList<>();
        traverseActiveFilters(filter -> clauses.addAll(filter.getValue()));
        return clauses;
    }

    /**
     * Update the place to reflect the current status of the filter settings
     *
     * @param place The place to update
     */
    public void updatePlace(AbstractBasePlace place) {
        traverseActiveFilters(filter -> place.addParameter(filter.getParameterKey(), filter.isInvertFilter(), filter.getParameter()));
    }

    private void removeChangeHandler() {
        changeHandler = null;
    }

    private class SubmitterFilterChangeEvent extends ChangeEvent {
    }

    private void valueChanged() {
        if (changeHandler != null) {
            changeHandler.onChange(new SubmitterFilterChangeEvent());
        }
    }

    /**
     * Traverses through the list of active filters (filters that are actively used), and calls
     * the functional interface Consumer on each element.
     *
     * @param action The functional interface to call, for each found active filter
     */
    private void traverseActiveFilters(Consumer<BaseSubmitterFilter> action) {
        for (Widget widget : submitterFilterContainer) {
            if (widget instanceof SubmitterFilterPanel) {
                SubmitterFilterPanel submitterFilterPanel = (SubmitterFilterPanel) widget;
                Iterator<Widget> submitterFilterIterator = submitterFilterPanel.iterator();  // Inner level: Find BaseSubmitterFilters - or any derivative
                if (submitterFilterIterator.hasNext()) {
                    Widget submitterFilterWidget = submitterFilterIterator.next();
                    if (submitterFilterWidget instanceof BaseSubmitterFilter) {
                        action.accept((BaseSubmitterFilter) submitterFilterWidget);
                    }
                }
            }
        }
    }

    /**
     * Sets up the filters according to the supplied list of URL Parameters<br>
     * Also removes any filters, not given in the supplied list
     *
     * @param urlParameters The URL parameters used as input
     */
    private void setNewUrlParameters(Map<String, AbstractBasePlace.PlaceParameterValue> urlParameters) {
        // First remove (de-attach) all filters, that aren't attached and not mentioned in the URL parameter list
        instantiatedFilters.forEach((name, filter) -> {
            if (!urlParameters.containsKey(name)) {
                filter.removeFilter(false);
            }
        });
        // Then do attach (add) all filters in the URL parameter list and setup parameters
        urlParameters.forEach((name, parameter) -> {
            if (instantiatedFilters.containsKey(name)) {
                BaseSubmitterFilter filter = instantiatedFilters.get(name);
                filter.instantiateFilter(false);
                filter.setParameter(parameter.isInvert(), parameter.getValue());
            }
        });
    }
}
