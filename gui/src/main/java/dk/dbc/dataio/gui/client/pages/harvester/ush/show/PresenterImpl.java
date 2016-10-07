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

package dk.dbc.dataio.gui.client.pages.harvester.ush.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.pages.harvester.ush.modify.EditPlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;

import java.util.List;


/**
 * This class represents the show harvesters presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {
    private final String RELATIVE_USH_ADMIN_URL = "/../harvester-admin/";
    protected String ushAdminUrl = "";

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    PlaceController placeController;

    /**
     * Default constructor
     * @param placeController The place controller
     */
    public PresenterImpl(PlaceController placeController) {
        this.placeController = placeController;
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
        getView().setHeader(commonInjector.getMenuTexts().menu_UshHarvesters());
        containerWidget.setWidget(getView().asWidget());
        fetchHarvesters();
        fetchUshAdminUrl();
    }


    /**
     * Overridden methods
     */

    /**
     * This method starts the edit harvester page
     * @param id The id of the harvester configuration to edit
     */
    @Override
    public void editHarvesterConfig(String id) {
        this.placeController.goTo(new EditPlace(id));
    }

    /**
     * This method runs a Ush Solr Test Harvest
     * @param id The id of the harvester to test run
     */
    @Override
    public void runUshSolrTestHarvest(long id) {
        commonInjector.getUshSolrHarvesterProxyAsync().runTestHarvest((int)id, new RunUshSolrTestHarvesterCallback());
    }

    /**
     * Opens a new window, containing the USH Harvester Admin Page
     */
    @Override
    public void openUshAdminPage() {
        Window.open(ushAdminUrl, "_blank", "");
    }


    /*
     * Private methods
     */

    /**
     * This method fetches all harvesters, and sends them to the view
     */
    private void fetchHarvesters() {
        commonInjector.getFlowStoreProxyAsync().findAllUshSolrHarvesterConfigs(new GetUshHarvestersCallback());
    }

    private void fetchUshAdminUrl() {
        commonInjector.getJndiProxyAsync().getJndiResource(JndiConstants.URL_RESOURCE_USH_HARVESTER, new GetUshAdminUrlCallback());
    }

    private View getView() {
        return viewInjector.getView();
    }


    // Local classes

    /**
     * This class is the callback class for the getJndiResource method in the JNDI Proxy
     */
    protected class GetUshAdminUrlCallback implements AsyncCallback<String> {
        @Override
        public void onFailure(Throwable throwable) {
            getView().setErrorText(viewInjector.getTexts().error_JndiFetchError());
        }
        @Override
        public void onSuccess(String jndiUrl) {
            ushAdminUrl = jndiUrl + RELATIVE_USH_ADMIN_URL;
        }
    }

    /**
     * This class is the callback class for the findAllFlows method in the Flow Store
     */
    protected class GetUshHarvestersCallback extends FilteredAsyncCallback<List<UshSolrHarvesterConfig>> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(caught, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }
        @Override
        public void onSuccess(List<UshSolrHarvesterConfig> ushHarvesterConfigs) {
            getView().setHarvesters(ushHarvesterConfigs);
        }
    }

    /**
     * This class is the callback class for the runTestHarvest method in the UshSolrHarvester Proxy
     */
    protected class RunUshSolrTestHarvesterCallback implements AsyncCallback<String> {
        @Override
        public void onFailure(Throwable caught) {
            getView().setErrorText(viewInjector.getTexts().error_RunUshSolrTestError());
        }
        @Override
        public void onSuccess(String harvestId) {
        }
    }
}
