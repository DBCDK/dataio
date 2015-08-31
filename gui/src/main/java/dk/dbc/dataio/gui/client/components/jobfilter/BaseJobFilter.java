package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

/**
 * This is the base class for Job Filters
 */
public abstract class BaseJobFilter extends Composite implements HasChangeHandlers {

    protected Texts texts;
    protected Resources resources;

    protected final Widget thisAsWidget = this.asWidget();
    protected JobFilter parentJobFilter = null;
    protected JobFilterPanel filterPanel = null;
    protected HandlerRegistration clickHandlerRegistration = null;

    /**
     * Constructor
     * @param texts Internationalized texts to be used by this class
     * @param resources Resources to be used by this class
     */
    @Inject
    public BaseJobFilter(Texts texts, Resources resources) {
        this.texts = texts;
        this.resources = resources;
    }

    /**
     * This is the abstract method, to be used for naming the actual Job Filter
     * @return The actual name of the Job Filter
     */
    public abstract String getName();


    /**
     * This method codes the behavior when adding the actual Job Filter (activating the menu)
     * @param parentJobFilter The JobFilter, where the current JobFilter is being added to
     * @return The Scheduler command to be used, when adding the Job Filter
     */
    public Scheduler.ScheduledCommand getAddCommand(final JobFilter parentJobFilter) {
        if (parentJobFilter == null) {
            return null;
        } else {
            this.parentJobFilter = parentJobFilter;
            return new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    addJobFilter();
                }
            };
        }
    }

    /**
     * Adds a Job Filter to the list of active filters. If the actual filter has already been added, nothing will happen.
     * Apart from adding the Job Filter, two handlers are registered:
     *  - A Click Handler is registered to assure, that a click on the remove button will remove the filter.
     *  - A Change Handler is registered to signal changes in the Job Filter to the owner panel
     */
    public void addJobFilter() {
        if (filterPanel == null) {
            GWT.log("Add Job Filter: " + getName());
            filterPanel = new JobFilterPanel(getName(), resources.deleteButton());
            clickHandlerRegistration = filterPanel.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    removeJobFilter();
                }
            });
            filterPanel.add(thisAsWidget);
            parentJobFilter.add(this);
        }
    }

    /**
     * Removes the Job Filter from the list of active filters.
     * The associated Click Handler is de-registered to assure, that no ghost events will be triggered
     */
    public void removeJobFilter() {
        GWT.log("Remove Job Filter: " + getName());
        if (filterPanel != null) {
            clickHandlerRegistration.removeHandler();
            clickHandlerRegistration = null;
            filterPanel.clear();
            parentJobFilter.remove(this);
        }
    }

    /**
     * Gets the value of the current Job List Criteria Model
     * @return The current value of the Job List Criteria Model
     */
    abstract public JobListCriteria getValue();

}
