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

package dk.dbc.dataio.gui.client.pages.navigation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import dk.dbc.dataio.gui.client.pages.job.show.ShowJobsPlace;
import dk.dbc.dataio.gui.client.pages.job.show.ShowTestJobsPlace;

public class NavigationPanel extends FlowPanel {
    interface NavigationBinder extends UiBinder<HTMLPanel, NavigationPanel> {}
    private static NavigationBinder uiBinder = GWT.create(NavigationBinder.class);

    private final PlaceController placeController;

    @UiField Tree menu;
    @UiField TreeItem jobs;
    @UiField TreeItem testJobs;
    @UiField TreeItem submitters;
    @UiField TreeItem flows;
    @UiField TreeItem flowComponents;
    @UiField TreeItem flowBinders;
    @UiField TreeItem sinks;
    @UiField TreeItem harvesters;
    @UiField TreeItem ioTraffic;
    @UiField TreeItem ftp;


    /**
     * Constructor for the NavigationPanel
     *
     * @param placeController The placecontroller to use, when navigating
     */
    public NavigationPanel(PlaceController placeController) {
        super();
        this.placeController = placeController;
        add(uiBinder.createAndBindUi(this));
        jobs.setUserObject(new ShowJobsPlace());
        testJobs.setUserObject(new ShowTestJobsPlace());
        flowBinders.setUserObject(new dk.dbc.dataio.gui.client.pages.flowbinder.show.Place());
        flows.setUserObject(new dk.dbc.dataio.gui.client.pages.flow.show.Place());
        flowComponents.setUserObject(new dk.dbc.dataio.gui.client.pages.flowcomponent.show.Place());
        harvesters.setUserObject(new dk.dbc.dataio.gui.client.pages.harvester.show.Place());
        submitters.setUserObject(new dk.dbc.dataio.gui.client.pages.submitter.show.Place());
        sinks.setUserObject(new dk.dbc.dataio.gui.client.pages.sink.show.Place());
        ioTraffic.setUserObject(new dk.dbc.dataio.gui.client.pages.ftp.show.Place());
        ftp.setUserObject(new dk.dbc.dataio.gui.client.pages.ftp.show.Place());
    }

    /**
     * Event handler for menu navigation events
     *
     * @param event The triggering event
     */
    @UiHandler("menu")
    void menuPressed(SelectionEvent<TreeItem> event) {
        Place place = (Place) event.getSelectedItem().getUserObject();
        if (placeController != null && place != null) {
            placeController.goTo(place);
        }
    }

}
