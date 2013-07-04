/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.util;

import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.places.FlowEditPlace;
import dk.dbc.dataio.gui.views.FlowEditView;
import dk.dbc.dataio.gui.views.FlowEditViewImpl;

/**
 *
 * @author slf
 */
public class ClientFactoryImpl implements ClientFactory {
    private final EventBus eventBus = new SimpleEventBus();
    private FlowEditView flowEditView = new FlowEditViewImpl();
    private PlaceController placeController = new PlaceController(eventBus);

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public FlowEditView getFlowEditView() {
        return flowEditView;
    }

    @Override
    public PlaceController getPlaceController() {
        return placeController;
    }
}
