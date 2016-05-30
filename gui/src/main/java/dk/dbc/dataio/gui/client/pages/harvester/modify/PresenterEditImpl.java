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

package dk.dbc.dataio.gui.client.pages.harvester.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;

import java.util.List;


/**
 * Concrete Presenter Implementation Class for Harvester Edit
 */
public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {
    private long id;

    /**
     * Constructor
     * @param place the edit place
     * @param header The header
     */
    public PresenterEditImpl(Place place, String header) {
        super(header);
        id = place.getHarvesterId();
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter
     * @param containerWidget the widget to use
     * @param eventBus the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
    }

    /**
     * Initializing the model
     * The method fetches the stored Harvester Configuration, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        commonInjector.getFlowStoreProxyAsync().getHarvesterRrConfigs(new GetHarvesterRrConfigsAsyncCallback());
    }

    /**
     * saveModel
     * Updates the embedded model as a Harvester Config in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().updateHarvesterRrConfig(config, new UpdateHarvesterRrConfigAsyncCallback());
    }



    /*
     * Private classes
     */

    class GetHarvesterRrConfigsAsyncCallback implements AsyncCallback<List<RRHarvesterConfig>> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "RRHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }
        @Override
        public void onSuccess(List<RRHarvesterConfig> rrHarvesterConfigs) {
            RRHarvesterConfig config = fetchHarvesterConfig(id, rrHarvesterConfigs);
            if (config == null) {
                getView().setErrorText(getTexts().error_HarvesterNotFound());
            } else {
                setRRHarvesterConfig(config);
                updateAllFieldsAccordingToCurrentState();
            }
        }
    }

    class UpdateHarvesterRrConfigAsyncCallback implements AsyncCallback<RRHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "RRHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }
        @Override
        public void onSuccess(RRHarvesterConfig rrHarvesterConfig) {
            getView().status.setText(getTexts().status_ConfigSuccessfullySaved());
            History.back();
        }
    }


    /*
     * Private methods
     */

    private RRHarvesterConfig fetchHarvesterConfig(long id, List<RRHarvesterConfig> rrHarvesterConfigs) {
        for (RRHarvesterConfig config: rrHarvesterConfigs) {
            if (config.getId() == id) {
                return config;
            }
        }
        return null;
    }
}
