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

package dk.dbc.dataio.gui.client.pages.flow.modify;


import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Flow Create
 */
public class PresenterCreateImpl extends PresenterImpl {

    /**
     * Constructor
     * @param clientFactory, clientFactory
     */
    public PresenterCreateImpl(ClientFactory clientFactory) {
        super(clientFactory);
        view = clientFactory.getFlowCreateView();
    }

    /**
     * initializeModel - initializes the model
     * When starting the form, the fields shall be empty, therefore an empty Model is instantiated
     */
    @Override
    public void initializeModel() {
        model = new FlowModel();
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * saveModel
     * Saves the embedded model as a new Flow in the database
     */
    @Override
    void saveModel() {
        flowStoreProxy.createFlow(model, new SaveFlowModelAsyncCallback());
    }

    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {}
}
