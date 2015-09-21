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

package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import dk.dbc.dataio.gui.client.components.JobNotificationPanel;

public class JobNotificationsTabContent extends Composite {
    interface JobDiagnosticTabContentUiBinder extends UiBinder<HTMLPanel, JobNotificationsTabContent> {
    }

    private static JobDiagnosticTabContentUiBinder ourUiBinder = GWT.create(JobDiagnosticTabContentUiBinder.class);

    public JobNotificationsTabContent() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    // UI Fields
    @UiField FlowPanel jobNotificationContainer;


    /**
     * Ui Handler to catch click events on the Back button
     * @param event Clicked event
     */
    @UiHandler("backButton")
    void backButtonPressed(ClickEvent event) {
        History.back();
    }

    /*
     * Public methods
     */

    /**
     * Get the number of Job Notifications in the panel
     * @return Number of Job Notifications in the panel
     */
    public int getNotificationsCount() {
        return jobNotificationContainer.getWidgetCount();
    }

    /**
     * Adds a Job Notification panel to the container
     * @param panel The Job Notification panel to add to the container
     */
    public void add(JobNotificationPanel panel) {
        jobNotificationContainer.add(panel);
    }

    /**
     * Clears all panels from the container
     */
    public void clear() {
        jobNotificationContainer.clear();
    }

}