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
    @UiField TreeItem rrHarvesters;
    @UiField TreeItem ushHarvesters;
    @UiField TreeItem gatekeeper;
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
        harvesters.setUserObject(rrHarvesters);
        rrHarvesters.setUserObject(new dk.dbc.dataio.gui.client.pages.harvester.rr.show.Place());
        ushHarvesters.setUserObject(null);
        submitters.setUserObject(new dk.dbc.dataio.gui.client.pages.submitter.show.Place());
        sinks.setUserObject(new dk.dbc.dataio.gui.client.pages.sink.show.Place());
        gatekeeper.setUserObject(ioTraffic);
        ioTraffic.setUserObject(new dk.dbc.dataio.gui.client.pages.iotraffic.Place());
        ftp.setUserObject(new dk.dbc.dataio.gui.client.pages.ftp.show.Place());
    }

    /**
     * Event handler for menu navigation events
     *
     * @param event The triggering event
     */
    @UiHandler("menu")
    void menuPressed(SelectionEvent<TreeItem> event) {
        doSelect(event.getSelectedItem());
    }

    /**
     * Make a selection
     * An action is activated, based on the type of the user object, embedded in the TreeItem passed as a parameter in the call to the method
     * If the user object is Place type, activate the Place
     * If the user object is a TreeItem, the tree item does not have a direct action
     * Instead, select the first item in the sub list (if any), and do the selection on this item instead - meaning
     * call this method recursively with the new object as parameter
     * @param item The item to do the selection upon
     */
    private void doSelect(TreeItem item) {
        clearAllSelected(menu);
        Object object = item.getUserObject();
        if (placeController != null && object != null) {
            if (object instanceof TreeItem) {
                doSelect((TreeItem) object);
            } else if (object instanceof Place) {
                placeController.goTo((Place) object);
                setSelection(item);
            }
        }
    }

    /**
     * Traverses through all tree items in the tree, and clears the selection on each of them
     * @param tree The tree, containing the tree items
     */
    private void clearAllSelected(Tree tree) {
        int count = tree.getItemCount();
        for (int i=0; i<count; i++) {
            clearTreeItemSelection(tree.getItem(i));
        }
    }

    /**
     * Clears the selection. If the item contains children, each one of them are cleared also (using recursion)
     * @param item The tree item to clear the selection
     */
    private void clearTreeItemSelection(TreeItem item) {
        item.setSelected(false);
        int count = item.getChildCount();
        for (int i=0; i<count; i++) {
            clearTreeItemSelection(item.getChild(i));
        }
    }

    /**
     * Sets the selection on the item, passed as a parameter
     * If the item has parents, it is checked, whether the item is displayed
     * @param item The item to set as selected
     */
    private void setSelection(TreeItem item) {
        item.setSelected(true);
        setParentUncovered(item);
    }

    /**
     * Assure that the item is visible - ie. its parent is not folded
     * @param item The item to check for visibility
     */
    private void setParentUncovered(TreeItem item) {
        if (item != null) {
            TreeItem parent = item.getParentItem();
            if (parent != null) {
                parent.setState(true);
                setParentUncovered(parent);  // Assure that the grand-parent is also uncovered
            }
        }
    }
}
