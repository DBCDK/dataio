package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import dk.dbc.dataio.gui.client.components.TitledDecoratorPanelWithButton;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.resources.Resources;

/**
 * This is the base class for Job Filters
 */
public abstract class BaseJobFilter extends Composite implements HasValue<JobListCriteriaModel>  {

    static protected Texts texts;
    static protected Resources resources;

    protected final Widget thisAsWidget = this.asWidget();
    protected FlowPanel parentPanel = null;
    protected TitledDecoratorPanelWithButton decoratorPanel = null;
    protected HandlerRegistration clickHandlerRegistration = null;
    protected JobListCriteriaModel jobListCriteriaModel = new JobListCriteriaModel();

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
     * @param parentContainer The container panel, where the actual Job Filter is added
     * @return The Scheduler command to be used, when adding the Job Filter
     */
    public Scheduler.ScheduledCommand getAddCommand(final FlowPanel parentContainer) {
        if (parentContainer == null) {
            return null;
        } else {
            parentPanel = parentContainer;
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
     * Apart from adding the Job Filter, a Click Handler is registered to assure, that a click on the remove
     * button will remove the filter.
     */
    public void addJobFilter() {
        if (clickHandlerRegistration == null) {
            decoratorPanel = new TitledDecoratorPanelWithButton(texts.sinkFilter_name(), resources.deleteButton());
            clickHandlerRegistration = decoratorPanel.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    removeJobFilter();
                }
            });
            decoratorPanel.add(thisAsWidget);
            parentPanel.add(decoratorPanel);
        }
    }

    /**
     * Removes the Job Filter from the list of active filters.
     * The associated Click Handler is de-registered to assure, that no ghost events will be triggered
     */
    public void removeJobFilter() {
        if (clickHandlerRegistration != null) {
            clickHandlerRegistration.removeHandler();
            clickHandlerRegistration = null;
            if (parentPanel != null) {
                parentPanel.remove(decoratorPanel);
            }
        }
    }

    /*
     * Override HasValue Interface Methods
     */

    @Override
    public JobListCriteriaModel getValue() {
        return jobListCriteriaModel;
    }

    @Override
    public void setValue(JobListCriteriaModel model) {
        jobListCriteriaModel = model;
    }

    @Override
    public void setValue(JobListCriteriaModel model, boolean fireEvents) {
        setValue(model);
        if (fireEvents) {
            ValueChangeEvent.fire(this, model);
        }
    }

}
