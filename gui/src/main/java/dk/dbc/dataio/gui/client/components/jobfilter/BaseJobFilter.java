package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.components.TitledDecoratorPanelWithButton;
import dk.dbc.dataio.gui.client.resources.Resources;

/**
 * This is the base class for Job Filters
 */
public abstract class BaseJobFilter extends Composite {
    protected final static Texts texts = GWT.create(Texts.class);  // Consider using Dependency Injection !!!
    protected final static Resources resources = GWT.create(Resources.class);  // Consider using Dependency Injection !!!
    protected final Widget thisAsWidget = this.asWidget();
    protected boolean hasBeenAdded = false;

    /**
     * This is the abstract method, to be used for naming the actual Job Filter
     * @return The actual name of the Job Filter
     */
    abstract public String getName();


    /**
     * This method codes the behavior when adding the actual Job Filter (activating the menu)
     * @param parentContainer The container panel, where the actual Job Filter is added
     * @return The Scheduler command to be used, when adding the Job Filter
     */
    public Scheduler.ScheduledCommand getAddCommand(final FlowPanel parentContainer) {
        return new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                if (!hasBeenAdded) {
                    hasBeenAdded = true;
                    TitledDecoratorPanelWithButton decoratorPanel = new TitledDecoratorPanelWithButton(texts.sinkFilter_name(), resources.deleteButton());
                    decoratorPanel.add(thisAsWidget);
                    parentContainer.add(decoratorPanel);
                }
            }
        };
    }

    public void removeJobFilter() {
        hasBeenAdded = false;

    }
}
