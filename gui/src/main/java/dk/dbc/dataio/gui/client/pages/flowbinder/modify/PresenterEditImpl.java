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

package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;

/**
 * Concrete Presenter Implementation Class for Flow Binder Edit
 */
public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {

    private long id;
    /**
     * Constructor
     * @param place     the edit place
     * @param header    Breadcrumb header text
     */
    public PresenterEditImpl(Place place, String header) {
        super(header);
        id = place.getFlowBinderId();
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
        getView().deleteButton.setVisible(true);
    }

    /**
     * Initializing the model
     * The method fetches the stored Flow Binder, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        getFlowBinder(id);
    }

    /**
     * saveModel
     * Updates the embedded model as a Flow Binder in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().updateFlowBinder(model, new SaveFlowBinderModelFilteredAsyncCallback());
    }

    /**
     * Deletes the embedded model as a FlowBinder in the database
     */
    void deleteModel() {
        commonInjector.getFlowStoreProxyAsync().deleteFlowBinder(model.getId(), model.getVersion(), new DeleteFlowBinderModelFilteredAsyncCallback());
    }

    /**
     * A signal to the presenter, saying that the delete button has been pressed
     */
    public void deleteButtonPressed() {
        if (model != null) {
            deleteModel();
        }
    }

    // Private methods
    private void getFlowBinder(final long flowBinderId) {
        commonInjector.getFlowStoreProxyAsync().getFlowBinder(flowBinderId, new GetFlowBinderModelFilteredAsyncCallback());
    }

    /**
     * Call back class to be instantiated in the call to getFlowBinder in flowStore proxy
     */
    class GetFlowBinderModelFilteredAsyncCallback extends FilteredAsyncCallback<FlowBinderModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "Flowbinder.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }
        @Override
        public void onSuccess(FlowBinderModel model) {
            setFlowBinderModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }

    /**
     * Local call back class to be instantiated in the call to deleteFlowBinder in flowstore proxy
     */
    class DeleteFlowBinderModelFilteredAsyncCallback extends FilteredAsyncCallback<Void> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(Void aVoid) {
            getView().status.setText(getTexts().status_FlowBinderSuccessfullyDeleted());
            setFlowBinderModel(null);
            History.back();
        }
    }
}
