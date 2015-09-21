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

package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Flow Component Edit
 */
public class PresenterEditImpl extends PresenterImpl {
    private long id;

    /**
     * Constructor
     * @param place, the place
     * @param clientFactory the clientFactory
     */
    public PresenterEditImpl(Place place, ClientFactory clientFactory) {
        super(clientFactory);
        view = clientFactory.getFlowComponentEditView();
        EditPlace editPlace = (EditPlace) place;
        id = editPlace.getFlowComponentId();
    }
    /**
     * Initializing the model
     * The method fetches the stored flow component, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        getFlowComponent(id);
    }

    /**
     * saveModel
     * Updates the embedded model as a flow component in the database
     */
    @Override
    void saveModel() {
        flowStoreProxy.updateFlowComponent(model, new SaveFlowComponentModelFilteredAsyncCallback());
    }

    // Private methods
    private void getFlowComponent(final long flowComponentId) {
        flowStoreProxy.getFlowComponent(flowComponentId, new GetFlowComponentModelFilteredAsyncCallback());
    }

    /**
     * Call back class to be instantiated in the call to getFlowComponent in flowStoreProxy
     */
    class GetFlowComponentModelFilteredAsyncCallback extends FilteredAsyncCallback<FlowComponentModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "Flowcomponent.id: " + id;
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts, msg));
        }
        @Override
        public void onSuccess(FlowComponentModel model) {
            setFlowComponentModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }
}
