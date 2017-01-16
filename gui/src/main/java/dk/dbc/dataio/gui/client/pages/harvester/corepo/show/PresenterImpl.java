/*
 *
 *  * DataIO - Data IO
 *  * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 *  * Denmark. CVR: 15149043
 *  *
 *  * This file is part of DataIO.
 *  *
 *  * DataIO is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * DataIO is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package dk.dbc.dataio.gui.client.pages.harvester.corepo.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;

import java.util.Date;
import java.util.List;


/**
 * This class represents the show harvesters presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
//    private PlaceController placeController;


    /**
     * Default constructor
     * @param placeController The placecontroller
     */
    @SuppressWarnings("unused")
    public PresenterImpl(PlaceController placeController) {
//        this.placeController = placeController;
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
        getView().setPresenter(this);
        getView().setHeader(commonInjector.getMenuTexts().menu_CoRepoHarvesters());
        containerWidget.setWidget(getView().asWidget());
        fetchHarvesters();
    }


    /*
     * Overridden interface methods
     */

//    /**
//     * This method starts the edit harvester page
//     * @param id The id of the harvester configuration to edit
//     */
//    @Override
//    public void editCoRepoHarvesterConfig(String id) {
//        this.placeController.goTo(new EditPlace(id));
//    }

//    /**
//     * This method starts the create harvester page
//     */
//    @Override
//    public void createCoRepoHarvester() {
//        placeController.goTo(new CreatePlace());
//    }


    /*
     * Private methods
     */

    /**
     * This method fetches all harvesters, and sends them to the view
     */
    private void fetchHarvesters() {
        commonInjector.getFlowStoreProxyAsync().findAllCoRepoHarvesterConfigs(new GetCoRepoHarvestersCallback());
    }

    private View getView() {
        return viewInjector.getView();
    }


    // Local classes

    /**
     * This class is the callback class for the findAllCoRepoHarvesterConfigs method in the Flow Store
     */
    class GetCoRepoHarvestersCallback extends FilteredAsyncCallback<List<CoRepoHarvesterConfig>> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(caught, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }
        @Override
        public void onSuccess(List<CoRepoHarvesterConfig> coRepoHarvesterConfigs) {
            coRepoHarvesterConfigs.add(new CoRepoHarvesterConfig(1L, 2L, new CoRepoHarvesterConfig.Content()
                    .withName("Name1")
                    .withDescription("This is the first")
                    .withResource("Res1")
                    .withTimeOfLastHarvest(new Date(1234567890L))
                    .withEnabled(true)
            ));
            coRepoHarvesterConfigs.add(new CoRepoHarvesterConfig(1L, 2L, new CoRepoHarvesterConfig.Content()
                    .withName("Number Two")
                    .withDescription("This is the second")
                    .withResource("Resu")
                    .withTimeOfLastHarvest(new Date(2234567890L))
                    .withEnabled(true)
            ));
            coRepoHarvesterConfigs.add(new CoRepoHarvesterConfig(1L, 2L, new CoRepoHarvesterConfig.Content()
                    .withName("Drei")
                    .withDescription("This is the third")
                    .withResource("Resus")
                    .withTimeOfLastHarvest(new Date(3234567890L))
                    .withEnabled(false)
            ));
            coRepoHarvesterConfigs.add(new CoRepoHarvesterConfig(1L, 2L, new CoRepoHarvesterConfig.Content()
                    .withName("Quattro")
                    .withDescription("This is the fourth")
                    .withResource("Resurvi")
                    .withTimeOfLastHarvest(new Date(4234567890L))
                    .withEnabled(true)
            ));
            getView().setHarvesters(coRepoHarvesterConfigs);
        }
    }

}
