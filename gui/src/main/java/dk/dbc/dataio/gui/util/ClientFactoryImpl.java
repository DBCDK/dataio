/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.util;

import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import dk.dbc.dataio.gui.client.views.FlowCreateView;
import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
import dk.dbc.dataio.gui.client.views.SubmitterCreateView;
import dk.dbc.dataio.gui.client.views.SubmitterCreateViewImpl;


/**
 *
 * @author slf
 */
public class ClientFactoryImpl implements ClientFactory {
    private final EventBus eventBus = new SimpleEventBus();
    private FlowCreateView flowCreateView = new FlowCreateViewImpl();
    private SubmitterCreateView submitterCreateView = new SubmitterCreateViewImpl();
    private PlaceController placeController = new PlaceController(eventBus);

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public FlowCreateView getFlowCreateView() {
        return flowCreateView;
    }

    @Override
    public SubmitterCreateView getSubmitterCreateView() {
        return submitterCreateView;
    }

    @Override
    public PlaceController getPlaceController() {
        return placeController;
    }
}
