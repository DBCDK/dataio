package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.events.JobFilterPanelEvent;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

/**
 * This is the base class for Job Filters
 */
public abstract class BaseJobFilter extends Composite implements HasChangeHandlers, Focusable {
    String parameterKeyName = getClass().getSimpleName();
    protected Texts texts;
    protected Resources resources;
    protected boolean initialInvertFilterValue;
    final private ClientFactory clientFactory = ClientFactoryImpl.getInstance();

    final Widget thisAsWidget = asWidget();
    JobFilter parentJobFilter = null;
    JobFilterPanel filterPanel = null;
    HandlerRegistration clickHandlerRegistration = null;

    /**
     * Constructor
     *
     * @param texts          Internationalized texts to be used by this class
     * @param resources      Resources to be used by this class
     * @param invertedFilter True: This is an inverted filter, False: This is not an inverted filter
     */
    public BaseJobFilter(Texts texts, Resources resources, boolean invertedFilter) {
        this.texts = texts;
        this.resources = resources;
        this.initialInvertFilterValue = invertedFilter;
    }

    /**
     * This method codes the behavior when adding the actual Job Filter (activating the menu)
     *
     * @param parentJobFilter The JobFilter, where the current JobFilter is being added to
     * @return The Scheduler command to be used, when adding the Job Filter
     */
    Scheduler.ScheduledCommand getAddCommand(final JobFilter parentJobFilter) {
        if (parentJobFilter == null) {
            return null;
        } else {
            this.parentJobFilter = parentJobFilter;
            return () -> instantiateJobFilter(true);
        }
    }


    /**
     * Instantiates this Job Filter and adds it to the list of active filters. If the actual filter has already been added, nothing will happen. <br>
     * Apart from adding the Job Filter, a Click Handler is registered to assure, that a click on any of the buttons will trigger an action.
     *
     * @param notifyPlace Determines whether to notify the place about the changes
     */
    public void instantiateJobFilter(boolean notifyPlace) {
        if (filterPanel == null) {
            filterPanel = new JobFilterPanel(getName(), resources, initialInvertFilterValue);
            clickHandlerRegistration = filterPanel.addJobFilterPanelHandler(event -> handleFilterPanelEvent(event.getJobFilterPanelButton()));
            filterPanel.add(thisAsWidget);
            parentJobFilter.add(this);
            if (notifyPlace) {
                filterChanged();
            }
            Scheduler.get().scheduleDeferred(() -> setFocus(true));
        }
    }

    /**
     * Sets the parameter value for the filter<br>
     * The default implementation assumes, that the filter is always active, and there is therefore no value to set
     *
     * @param filterParameter The filter parameter for the specific job filter
     */
    public void setParameter(String filterParameter) {
        localSetParameter(filterParameter);
    }

    /**
     * Sets the parameter value for the filter<br>
     * The default implementation assumes, that the filter is always active, and there is therefore no value to set
     *
     * @param inverted        True if filter is inverted, false if not
     * @param filterParameter The filter parameter for the specific job filter
     */
    public void setParameter(boolean inverted, String filterParameter) {
        if (filterPanel != null) {
            filterPanel.setInvertFilter(inverted);
        }
        localSetParameter(filterParameter);
    }

    /**
     * Removes the Job Filter from the list of active filters.
     * The associated Click Handler is de-registered to assure, that no ghost events will be triggered
     *
     * @param notifyPlace Determines whether to notify the place about the changes
     */
    void removeJobFilter(boolean notifyPlace) {
        if (filterPanel != null) {
            clickHandlerRegistration.removeHandler();
            clickHandlerRegistration = null;
            filterPanel.clear();
            parentJobFilter.remove(this);
            if (notifyPlace) {
                filterChanged();
            }
        }
    }

    /**
     * This method shall be called by subclasses, whenever the filter changes its value <br>
     * The action is to reflect any changes in the URL
     */
    void filterChanged() {
        if (parentJobFilter != null && parentJobFilter.place != null) {
            if (filterPanel == null) {  // If filterPanel IS null, it means, that it has been removed
                parentJobFilter.place.removeParameter(getParameterKeyName());
            } else {
                parentJobFilter.place.addParameter(getParameterKeyName(), isInvertFilter(), getParameter());
            }

            // Refresh the URL in the browser
            Scheduler.get().scheduleDeferred(this::deferredRefreshPlace);  // It is not possible to fire an event directly from here - throws 3 AttachDetachException's
        }
    }

    /**
     * Test whether this is an inverted filter.
     *
     * @return True if the filter is an inverted, false not
     */
    boolean isInvertFilter() {
        return filterPanel == null || filterPanel.isInvertFilter();
    }


    /*
     * Local methods
     */

    /**
     * Handles a Filter Panel Event, and takes action upon it
     *
     * @param button The button event
     */
    void handleFilterPanelEvent(JobFilterPanelEvent.JobFilterPanelButton button) {
        switch (button) {
            case REMOVE_BUTTON:
                removeJobFilter(true);
                break;
            case PLUS_BUTTON:
            case MINUS_BUTTON:
                filterChanged();
                break;
            default:
                break;
        }
    }

    /**
     * Fires a PlaceChangeEvent, signalling changes in the place<br>
     * This causes the URL in the browser to be refreshed to the current content of the place
     */
    private void deferredRefreshPlace() {
        clientFactory.getEventBus().fireEvent(new PlaceChangeEvent(parentJobFilter.place));
    }


    /*
     * Empty default implementation of the Focusable Interface
     * To be overridden if a specific implementation is wanted.
     */

    /**
     * Gets the widget's position in the tab index.
     *
     * @return the widget's tab index
     */
    public int getTabIndex() {
        // No default implementation
        return 0;
    }

    /**
     * Sets the widget's 'access key'. This key is used (in conjunction with a browser-specific modifier key) to automatically focus the widget.
     *
     * @param accessKey the widget's access key
     */
    public void setAccessKey(char accessKey) {
        // No default implementation
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus at a time, and the widget that does will receive all keyboard events.
     *
     * @param focused whether this widget should take focus or release it
     */
    public void setFocus(boolean focused) {
        // No default implementation
    }

    /**
     * Sets the widget's position in the tab index. If more than one widget has the same tab index, each such widget will receive focus in an arbitrary order. Setting the tab index to -1 will cause this widget to be removed from the tab order.
     *
     * @param index the widget's tab index
     */
    public void setTabIndex(int index) {
        // No default implementation
    }


    /*
     * Default Overridable implementations
     */

    /**
     * Gets the parameter value for the filter<br>
     * The default implementation always returns an empty string, since the assumption is, that the filter is always active
     *
     * @return The stored filter parameter for the specific job filter
     */
    public String getParameter() {
        return "";
    }

    /**
     * Adds a ChangeHandler for this job filter<br>
     * The default implementation assumes, that there is no changeable input elements on the form, and there are consequently no changehandler to add, since no change handler is needed.
     *
     * @param changeHandler The ChangeHandler for this job filter
     * @return A HandlerRegistration object to be used to remove the job filter
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return () -> {
        };  // Returns a handler registration object, that is empty
    }

    /**
     * Returns the name of the parameter key
     *
     * @return The parameter key name
     */
    public String getParameterKeyName() {
        return parameterKeyName;
    }



    /*
     * Abstract Methods
     */

    /**
     * Sets the parameter value for the filter<br>
     *
     * @param filterParameter The filter parameter for the specific job filter
     */
    public abstract void localSetParameter(String filterParameter);

    /**
     * Gets the name of the actual Job Filter
     *
     * @return The name of the Job Filter
     */
    public abstract String getName();

    /**
     * Gets the value of the current Job List Criteria Model
     *
     * @return The current value of the Job List Criteria Model
     */
    abstract public JobListCriteria getValue();

}
