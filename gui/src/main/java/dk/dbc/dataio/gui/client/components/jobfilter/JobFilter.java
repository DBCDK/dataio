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
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

import java.util.Iterator;

/**
 * This class implements the generic Jobs Filter as a UI Binder component.
 * To be added in the top of the Jobs List.
 * The component contains an "Add Filter" menu and a button to be used to activate the filter:
 *
 * <pre>
 * <code>
 * +---------------+
 * | Tilføj Filter |
 * +---------------+
 * }
 * </code>
 * </pre>
 *
 * When the menu "Add Filter" is clicked, a sub menu will appear, containing the names of all available filters
 * These filters are configured in the {@link JobFilterList} class
 *
 * In UI Binder, add the following:
 *
 * <pre>
 * <code>
 *  &lt;jobs:JobFilter ui:field="jobFilter" /&gt;
 * </code>
 * </pre>
 */
public class JobFilter extends Composite implements HasChangeHandlers {
    interface JobFilterUiBinder extends UiBinder<HTMLPanel, JobFilter> {
    }

    private static JobFilterUiBinder ourUiBinder = GWT.create(JobFilterUiBinder.class);

    ChangeHandler changeHandler = null;

    @UiField FlowPanel jobFilterPanel;
    @UiField MenuBar filterMenu;


    /**
     * Default empty Constructor
     */
    @UiConstructor
    public JobFilter() {
        this(new JobFilterList());
    }

    /**
     * Constructor with list of Available Job Filters to be shown upon startup
     * @param availableJobFilters The list of Available Job Filters
     */
    public JobFilter(JobFilterList availableJobFilters) {
        initWidget(ourUiBinder.createAndBindUi(this));
        for (JobFilterList.JobFilterItem filter: availableJobFilters.getJobFilterList()) {
            filterMenu.addItem(filter.jobFilter.getName(), filter.jobFilter.getAddCommand(this));
            if (filter.activeOnStartup) {
                filter.jobFilter.getAddCommand(this).execute();
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
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                removeChangeHandler();
            }
        };
    }


    /*
     * Public Methods
     */

    /**
     * Adds a child Job Filter to the list of Job Filters
     * @param jobFilter The job filter to add to the list of Job Filters
     */
    public void add(BaseJobFilter jobFilter) {
        if (jobFilter != null) {
            jobFilterPanel.add(jobFilter.filterPanel);
            jobFilter.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent changeEvent) {
                    valueChanged();
                }
            });
            valueChanged();  // Do assure, that whenever a filter is being applied, do the filtering
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


    /*
     * Private methods and classes
     */
    private void removeChangeHandler() {
        changeHandler = null;
    }

    class JobFilterChangeEvent extends ChangeEvent {}

    void valueChanged() {
        if (changeHandler != null) {
            changeHandler.onChange(new JobFilterChangeEvent());
        }
    }

}