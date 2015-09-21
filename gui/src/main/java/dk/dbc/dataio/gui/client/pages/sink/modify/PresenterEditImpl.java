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

package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.History;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Sink Edit
 */
public class PresenterEditImpl extends PresenterImpl {
    private long id;

    /**
     * Constructor
     * @param place the edit place
     * @param clientFactory the client factory
     */
    public PresenterEditImpl(Place place, ClientFactory clientFactory) {
        super(clientFactory);
        view = clientFactory.getSinkEditView();
        EditPlace editPlace = (EditPlace) place;
        id = editPlace.getSinkId();
        view.deleteButton.setVisible(true);
    }
    /**
     * Initializing the model
     * The method fetches the stored Sink, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        getSink(id);
    }

    /**
     * saveModel
     * Updates the embedded model as a Sink in the database
     */
    @Override
    void saveModel() {
        flowStoreProxy.updateSink(model, new SaveSinkModelFilteredAsyncCallback());
    }

    /**
     * Deletes the embedded model as a Sink in the database
     */
    void deleteModel() {
        flowStoreProxy.deleteSink(model.getId(), model.getVersion(), new DeleteSinkModelFilteredAsyncCallback());
    }

    // Private methods
    private void getSink(final long sinkId) {
        flowStoreProxy.getSink(sinkId, new GetSinkModelFilteredAsyncCallback());
    }

    /**
     * A signal to the presenter, saying that the delete button has been pressed
     */
    public void deleteButtonPressed() {
        if (model != null) {
            deleteModel();
        }
    }

    /**
     * Call back class to be instantiated in the call to getSink in flowstore proxy
     */
    class GetSinkModelFilteredAsyncCallback extends FilteredAsyncCallback<SinkModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "Sink.id: " + id;
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts, msg));
        }

        @Override
        public void onSuccess(SinkModel model) {
            setSinkModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }

    /**
     * Local call back class to be instantiated in the call to deleteSink in flowstore proxy
     */
    class DeleteSinkModelFilteredAsyncCallback extends FilteredAsyncCallback<Void> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts, null));
        }

        @Override
        public void onSuccess(Void aVoid) {
            view.status.setText(texts.status_SinkSuccessfullyDeleted());
            setSinkModel(null);
            History.back();
        }
    }
}
