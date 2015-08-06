package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.components.TitledDecoratorPanelWithButton;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;

import java.util.Iterator;

/**
 * This class implements the generic Jobs Filter as a UI Binder component.
 * To be added in the top of the Jobs List.
 * The component contains an "Add Filter" menu and a button to be used to activate the filter:
 *
 * <pre>
 * <code>
 * +---------------+    +---------+
 * | Tilføj Filter |    | Filtrér |
 * +---------------+    +---------+
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
public class JobFilter extends Composite implements HasValue<JobListCriteriaModel>, HasClickHandlers {
    interface JobFilterUiBinder extends UiBinder<HTMLPanel, JobFilter> {
    }

    private static JobFilterUiBinder ourUiBinder = GWT.create(JobFilterUiBinder.class);

    @UiField FlowPanel jobFilterPanel;
    @UiField MenuBar filterMenu;
    @UiField Button filterButton;

    /**
     * Constructor
     */
    public JobFilter() {
        this(new JobFilterList());
    }

    public JobFilter(JobFilterList availableJobFilters) {
        initWidget(ourUiBinder.createAndBindUi(this));
        for (BaseJobFilter filter: availableJobFilters.getJobFilterList()) {
            filterMenu.addItem(filter.getName(), filter.getAddCommand(jobFilterPanel));
        }
    }


    /*
     * HasClickHandlers Interface Methods
     */

    @Override
    public HandlerRegistration addClickHandler(ClickHandler clickHandler) {
        return filterButton.addClickHandler(clickHandler);
    }


    /*
     * Has Value Interface Methods
     */

    @Override
    public JobListCriteriaModel getValue() {
        JobListCriteriaModel resultingJobListCriteriaModel = new JobListCriteriaModel();

        // Now do find all derivatives of the BaseJobFilter - eg SinkJobFilter, and get it's JobListCriteriaModel
        Iterator<Widget> decoratorPanelIterator = jobFilterPanel.iterator();  // Outer level: Find TitledDecoratorPanelWithButton's
        while (decoratorPanelIterator.hasNext()) {
            Widget decoratorPanelWidget = decoratorPanelIterator.next();
            if (decoratorPanelWidget instanceof TitledDecoratorPanelWithButton) {
                TitledDecoratorPanelWithButton titledDecoratorPanelWithButton = (TitledDecoratorPanelWithButton) decoratorPanelWidget;
                Iterator<Widget> baseJobFilterIterator = titledDecoratorPanelWithButton.iterator();  // Inner level: Find BaseJobFilter's - or any derivative
                if (baseJobFilterIterator.hasNext()) {
                    Widget baseJobFilterWidget = baseJobFilterIterator.next();
                    if (baseJobFilterWidget instanceof BaseJobFilter) {
                        BaseJobFilter baseJobFilter = (BaseJobFilter) baseJobFilterWidget;
                        JobListCriteriaModel model = baseJobFilter.getValue();
                        resultingJobListCriteriaModel.and(model);
                    }
                }
            }
        }
        return resultingJobListCriteriaModel;
    }

    @Override
    public void setValue(JobListCriteriaModel s) {
        // No implementation - it makes no sense
    }

    @Override
    public void setValue(JobListCriteriaModel s, boolean b) {
        // No implementation - it makes no sense
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<JobListCriteriaModel> valueChangeHandler) {
        return null;
    }


    /**
     * Enable or disable the filter button
     * @param enable True: enable button, false: disable button
     */
    public void enableFilterButton(boolean enable) {
        this.filterButton.setEnabled(enable);
    }

}