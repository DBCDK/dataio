/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.util;

import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.views.FlowEditView;

/**
 *
 * @author slf
 */
public interface ClientFactory {

    EventBus getEventBus();

    FlowEditView getFlowEditView();

    public PlaceController getPlaceController();
}
