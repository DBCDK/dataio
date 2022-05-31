package dk.dbc.dataio.gui.client.components.flowbinderfilter;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.events.FlowBinderFilterPanelEvent;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;

import java.util.List;

/**
 * This is the base class for FLowBinder filters
 */
public abstract class BaseFlowBinderFilter extends Composite implements HasChangeHandlers, Focusable {
    String parameterKey = getClass().getSimpleName();
    protected Texts texts;
    protected Resources resources;
    protected boolean initialInvertFilterValue;

    final Widget thisAsWidget = asWidget();
    FlowBinderFilter parentFilter = null;
    FlowBinderFilterPanel filterPanel = null;
    HandlerRegistration clickHandlerRegistration = null;
    final private ClientFactory clientFactory = ClientFactoryImpl.getInstance();

    /**
     * Constructor
     *
     * @param texts          Internationalized texts to be used by this class
     * @param resources      Resources to be used by this class
     * @param invertedFilter True: This is an inverted filter, False: This is not an inverted filter
     */
    public BaseFlowBinderFilter(Texts texts, Resources resources, boolean invertedFilter) {
        this.texts = texts;
        this.resources = resources;
        this.initialInvertFilterValue = invertedFilter;
    }

    /**
     * This method codes the behavior when adding the actual filter (activating the menu)
     *
     * @param parentJobFilter parent filter where the current filter is being added to
     * @return scheduler command to be used when adding the filter
     */
    Scheduler.ScheduledCommand getAddCommand(final FlowBinderFilter parentJobFilter) {
        if (parentJobFilter == null) {
            return null;
        } else {
            this.parentFilter = parentJobFilter;
            return () -> instantiateFilter(true);
        }
    }


    /**
     * Instantiates this filter and adds it to the list of active filters.
     * If the actual filter has already been added, nothing will happen. <br>
     * Apart from adding the filter, a Click Handler is registered to assure,
     * that a click on any of the buttons will trigger an action.
     *
     * @param notifyPlace Determines whether to notify the place about the changes
     */
    public void instantiateFilter(boolean notifyPlace) {
        if (filterPanel == null) {
            filterPanel = new FlowBinderFilterPanel(getName(), resources, initialInvertFilterValue);
            clickHandlerRegistration = filterPanel.addFlowBinderFilterPanelHandler(event -> handleFilterPanelEvent(event.getFilterPanelButton()));
            filterPanel.add(thisAsWidget);
            parentFilter.add(this);
            if (notifyPlace) {
                filterChanged();
            }
            Scheduler.get().scheduleDeferred(() -> setFocus(true));
        }
    }

    /**
     * Sets the parameter value for the filter<br>
     * The default implementation assumes, that the filter is always active,
     * and there is therefore no value to set
     *
     * @param filterParameter filter parameter for the specific filter
     */
    public void setParameter(String filterParameter) {
        localSetParameter(filterParameter);
    }

    /**
     * Sets the parameter value for the filter<br>
     * The default implementation assumes, that the filter is always active,
     * and there is therefore no value to set
     *
     * @param inverted        True if filter is inverted, false if not
     * @param filterParameter filter parameter for the specific filter
     */
    public void setParameter(boolean inverted, String filterParameter) {
        if (filterPanel != null) {
            filterPanel.setInvertFilter(inverted);
        }
        localSetParameter(filterParameter);
    }

    /**
     * Removes this filter from the list of active filters.
     * The associated Click Handler is de-registered to assure,
     * that no ghost events will be triggered
     *
     * @param notifyPlace Determines whether to notify the place about the changes
     */
    void removeFilter(boolean notifyPlace) {
        if (filterPanel != null) {
            clickHandlerRegistration.removeHandler();
            clickHandlerRegistration = null;
            filterPanel.clear();
            parentFilter.remove(this);
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
        if (parentFilter != null && parentFilter.place != null) {
            if (filterPanel == null) {  // If filterPanel IS null, it means, that it has been removed
                parentFilter.place.removeParameter(getParameterKey());
            } else {
                parentFilter.place.addParameter(getParameterKey(), isInvertFilter(), getParameter());
            }

            // Refresh the URL in the browser
            // It is not possible to fire an event directly from here - throws 3 AttachDetachExceptions

            // This is currently NOT working - the URL remains unchanged ???
            Scheduler.get().scheduleDeferred(this::deferredRefreshPlace);
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

    /**
     * Handles a Filter Panel Event, and takes action upon it
     *
     * @param button The button event
     */
    void handleFilterPanelEvent(FlowBinderFilterPanelEvent.FilterPanelButton button) {
        switch (button) {
            case REMOVE_BUTTON:
                removeFilter(true);
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
        clientFactory.getEventBus().fireEvent(new PlaceChangeEvent(parentFilter.place));
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
     * Sets the widget's 'access key'. This key is used (in conjunction with
     * a browser-specific modifier key) to automatically focus the widget.
     *
     * @param accessKey the widget's access key
     */
    public void setAccessKey(char accessKey) {
        // No default implementation
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus at a time,
     * and the widget that does will receive all keyboard events.
     *
     * @param focused whether this widget should take focus or release it
     */
    public void setFocus(boolean focused) {
        // No default implementation
    }

    /**
     * Sets the widget's position in the tab index. If more than one widget has the same tab index,
     * each such widget will receive focus in an arbitrary order. Setting the tab index to -1 will
     * cause this widget to be removed from the tab order.
     *
     * @param index the widget's tab index
     */
    public void setTabIndex(int index) {
        // No default implementation
    }

    /**
     * Gets the parameter value for the filter<br>
     * The default implementation always returns an empty string,
     * since the assumption is that the filter is always active
     *
     * @return stored filter parameter for the specific filter
     */
    public String getParameter() {
        return "";
    }

    /**
     * Adds a ChangeHandler for this filter<br>
     * The default implementation assumes that there is no changeable input elements on the form,
     * and there are consequently no change handler to add since it is not needed.
     *
     * @param changeHandler ChangeHandler for this filter
     * @return HandlerRegistration object to be used to remove the filter
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return () -> {
        };  // Returns a handler registration object, that is empty
    }

    /**
     * @return parameter key
     */
    public String getParameterKey() {
        return parameterKey;
    }

    /**
     * Sets the parameter value for the filter
     *
     * @param filterParameter filter parameter for the specific filter
     */
    public abstract void localSetParameter(String filterParameter);

    /**
     * @return name of filter
     */
    public abstract String getName();

    /**
     * @return list of query clauses
     */
    abstract public List<GwtQueryClause> getValue();

}
