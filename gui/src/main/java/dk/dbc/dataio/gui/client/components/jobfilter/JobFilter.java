package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.MenuBar;

/**
 * This class implements the generic Jobs Filter as a UI Binder component.
 * To be added in the top of the Jobs List.
 * The component contains an "Add Filter" menu and a button to be used to activate the filter:
 *
 * <pre>
 * @{code
 * +---------------+    +---------+
 * | Tilføj Filter |    | Filtrér |
 * +---------------+    +---------+
 * }
 * </pre>
 *
 * When the menu "Add Filter" is clicked, a sub menu will appear, containing the names of all available filters
 * These filters are configured in the {@link JobFilterList} class
 *
 * In UI Binder, add the following:
 *
 * <pre>
 * @{code
 *  <jobs:JobFilter ui:field="jobFilter" />
 * }
 * </pre>
 */
public class JobFilter extends Composite {
    interface JobFilterUiBinder extends UiBinder<HTMLPanel, JobFilter> {
    }

    private static JobFilterUiBinder ourUiBinder = GWT.create(JobFilterUiBinder.class);

    @UiField FlowPanel jobFilterList;
    @UiField MenuBar filterMenu;
    @UiField Button filterButton;

    /**
     * Constructor
     */
    public JobFilter() {
        initWidget(ourUiBinder.createAndBindUi(this));
        for (BaseJobFilter filter: JobFilterList.getJobFilterList()) {
            filterMenu.addItem(filter.getName(), filter.getAddCommand(jobFilterList));
        }
    }

    /**
     * Enable or disable the filter button
     * @param enable True: enable button, false: disable button
     */
    public void enableFilterButton(boolean enable) {
        this.filterButton.setEnabled(enable);
    }


}