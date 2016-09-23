/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

/**
 * This is the base class for Job Filters
 */
public abstract class BaseJobFilter extends Composite implements HasChangeHandlers, Focusable {

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
     * This method codes the behavior when adding the actual Job Filter (activating the menu)
     * @param parentJobFilter The JobFilter, where the current JobFilter is being added to
     * @return The Scheduler command to be used, when adding the Job Filter
     */
    public Scheduler.ScheduledCommand getAddCommand(final JobFilter parentJobFilter) {
        if (parentJobFilter == null) {
            return null;
        } else {
            this.parentJobFilter = parentJobFilter;
            return this::addJobFilter;
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
            clickHandlerRegistration = filterPanel.addClickHandler(clickEvent -> removeJobFilter());
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


    /*
     * Empty default implementation of the Focusable Interface
     * To be overridden if a specific implementation is wanted.
     */

    /**
     * Gets the widget's position in the tab index.
     * @return the widget's tab index
     */
    public int getTabIndex() {
        // No default implementation
        return 0;
    }

    /**
     * Sets the widget's 'access key'. This key is used (in conjunction with a browser-specific modifier key) to automatically focus the widget.
     * @param accessKey the widget's access key
     */
    public void setAccessKey(char accessKey) {
        // No default implementation
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus at a time, and the widget that does will receive all keyboard events.
     * @param focused whether this widget should take focus or release it
     */
    public void setFocus(boolean focused) {
        // No default implementation
    }

    /**
     *  Sets the widget's position in the tab index. If more than one widget has the same tab index, each such widget will receive focus in an arbitrary order. Setting the tab index to -1 will cause this widget to be removed from the tab order.
     * @param index the widget's tab index
     */
    public void setTabIndex(int index) {
        // No default implementation
    }



    /*
     * Abstract Methods
     */


    /**
     * Gets the name of the actual Job Filter
     * @return The name of the Job Filter
     */
    public abstract String getName();


    /**
     * Gets the value of the current Job List Criteria Model
     * @return The current value of the Job List Criteria Model
     */
    abstract public JobListCriteria getValue();

}
