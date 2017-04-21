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

package dk.dbc.dataio.gui.client.pages.harvester.holdingsitem.modify;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.harvester.types.PhHoldingsItemsHarvesterConfig;

import java.util.ArrayList;

/**
 * Concrete Presenter Implementation Class for Harvester Edit
 */
public class PresenterCreateImpl<Place extends EditPlace> extends PresenterImpl {
    /**
     * Constructor
     * @param header The header
     */
    public PresenterCreateImpl(String header) {
        super(header);

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

        PhHoldingsItemsHarvesterConfig phHoldingsItemsHarvesterConfig = new PhHoldingsItemsHarvesterConfig(
                1,
                1,
                new PhHoldingsItemsHarvesterConfig.Content()
                        .withName("")
                        .withDescription("")
                        .withResource("")
                        .withRrHarvesters(new ArrayList<>())
                        .withEnabled(false)
            );
        setHoldingsItemHarvesterConfig(phHoldingsItemsHarvesterConfig);
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * saveModel
     * Updates the embedded model as a Harvester Config in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().createHoldingsItemHarvesterConfig(config, new CreateHarvesterConfigAsyncCallback());
    }


    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {}


    /*
     * Private classes
     */

    class CreateHarvesterConfigAsyncCallback implements AsyncCallback<PhHoldingsItemsHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "HarvesterConfig.id: [new Harvester]";
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }
        @Override
        public void onSuccess(PhHoldingsItemsHarvesterConfig harvesterConfig) {
            getView().status.setText(getTexts().status_ConfigSuccessfullySaved());
            History.back();
        }
    }

}
