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

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.VipSinkConfig;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.gui.client.model.SinkModel;

/**
 * Concrete Presenter Implementation Class for Sink Create
 */
public class PresenterCreateImpl extends PresenterImpl {

    private static final String DEFAULT_DUMMY_SINK_RESOURCE = "jdbc/dataio/dummy";

    /**
     * Constructor
     * @param header            header
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
        getView().deleteButton.setVisible(false);
    }

    /**
     * getModel - initializes the model
     * When starting the form, the fields should be empty, therefore an empty Model is instantiated
     */
    @Override
    public void initializeModel() {
        model = new SinkModel();
        View view = getView();
        view.sinkTypeSelection.fireChangeEvent();
        updateAllFieldsAccordingToCurrentState();
    }

    @Override
    void handleSinkConfig(SinkContent.SinkType sinkType) {
        View view = getView();
        view.sinkTypeSelection.setEnabled(true);
        model.setSinkType(sinkType);
        switch (sinkType) {
            case OPENUPDATE:
                model.setSinkConfig(new OpenUpdateSinkConfig());
                view.updateSinkSection.setVisible(true);
                break;
            case ES:
                model.setSinkConfig(new EsSinkConfig());
                view.esSinkSection.setVisible(true);
                break;
            case IMS:
                model.setSinkConfig(new ImsSinkConfig());
                view.imsSinkSection.setVisible(true);
                break;
            case WORLDCAT:
                model.setSinkConfig(new WorldCatSinkConfig());
                view.worldCatSinkSection.setVisible(true);
                break;
            case TICKLE:
                model.setSinkConfig(null);
                view.sequenceAnalysisSection.setVisible(false);
                break;
            case DUMMY:
                model.setSinkConfig(null);
                model.setResourceName(DEFAULT_DUMMY_SINK_RESOURCE);
                view.resource.setEnabled(false);
                view.resource.setValue(DEFAULT_DUMMY_SINK_RESOURCE);
                break;
            case VIP:
                model.setSinkConfig(new VipSinkConfig());
                view.vipSinkSection.setVisible(true);
                view.sequenceAnalysisSection.setVisible(false);
                break;
            default:
                model.setSinkConfig(null);
        }
    }

    /**
     * saveModel
     * Saves the embedded model as a new Sink in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().createSink(model, new SaveSinkModelFilteredAsyncCallback());
    }

    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {}
}
