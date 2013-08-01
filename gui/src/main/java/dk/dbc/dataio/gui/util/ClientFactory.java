package dk.dbc.dataio.gui.util;

import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowCreateView;
import dk.dbc.dataio.gui.client.views.SubmitterCreateView;

/**
 * Provides access to common objects across the application including:
 * - views as singleton objects, which improves performance since views contain DOM calls which are expensive.
 * - shared event bus.
 * - any RPC proxies.
 */
public interface ClientFactory {

    EventBus getEventBus();

    FlowCreateView getFlowCreateView();

    SubmitterCreateView getSubmitterCreateView();

    PlaceController getPlaceController();

    FlowStoreProxyAsync getFlowStoreProxyAsync();
}
