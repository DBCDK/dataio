package dk.dbc.dataio.gui.util;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.client.places.AppPlaceHistoryMapper;

/**
 * Provides access to common objects across the application including:
 * - views as singleton objects, which improves performance since views contain DOM calls which are expensive.
 * - shared event bus.
 * - any RPC proxies.
 */
public interface ClientFactory {

    // Factory for Global Views
    GlobalViewsFactory getGlobalViewsFactory();

    // Event Bus
    EventBus getEventBus();

    // Place Controller
    PlaceController getPlaceController();

    Activity getPresenter(AbstractBasePlace place);

    AppPlaceHistoryMapper getHistoryMapper();
}
