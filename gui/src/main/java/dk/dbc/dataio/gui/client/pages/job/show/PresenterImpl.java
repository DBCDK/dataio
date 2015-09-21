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

package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.view.client.Range;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;


/**
* This class represents the show jobs presenter implementation
*/
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    protected View view;
    protected JobStoreProxyAsync jobStoreProxy;
    private PlaceController placeController;

    /**
     * Default constructor
     *
     * @param clientFactory The client factory to be used
     */
    public PresenterImpl(ClientFactory clientFactory) {
        placeController = clientFactory.getPlaceController();
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
    }


    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        updateBaseQuery();
    }


    /*
     * Overrides
     */

    /**
     * This method is a result of a click on one job in the list, and activates the Item Show page
     * @param model The model, containing the selected item
     */
    @Override
    public void itemSelected(JobModel model) {
        placeController.goTo(new dk.dbc.dataio.gui.client.pages.item.show.Place(model.getJobId()));
    }

    @Override
    public void updateSelectedJobs() {
        view.selectionModel.clear();
        view.dataProvider.updateUserCriteria();
        view.dataProvider.updateCurrentCriteria();
        view.jobsTable.setVisibleRangeAndClearData(new Range(0, 20), true);
    }


    @Override
    public void refresh() {
        view.refreshJobsTable();
    }

    /**
     * Abstract Methods
     */

    protected abstract void updateBaseQuery();





}
