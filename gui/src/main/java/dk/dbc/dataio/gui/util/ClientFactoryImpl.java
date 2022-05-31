package dk.dbc.dataio.gui.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.client.places.AppPlaceHistoryMapper;

public class ClientFactoryImpl implements ClientFactory {

    private static final ClientFactoryImpl instance = new ClientFactoryImpl();

    static public final ClientFactoryImpl getInstance() {
        return instance;
    }

    private GlobalViewsFactory globalViewFactory = new GlobalViewsFactory();

    // Event Bus
    private final EventBus eventBus = new SimpleEventBus();

    // Place Controller
    private final PlaceController placeController = new PlaceController(eventBus);

    // History Mapper
    private final AppPlaceHistoryMapper historyMapper = GWT.create(AppPlaceHistoryMapper.class);

    private ClientFactoryImpl() {
    }

    @Override
    public GlobalViewsFactory getGlobalViewsFactory() {
        return globalViewFactory;
    }

    // Event Bus
    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    // Place Controller
    @Override
    public PlaceController getPlaceController() {
        return placeController;
    }

    // History Mapper
    @Override
    public AppPlaceHistoryMapper getHistoryMapper() {
        return historyMapper;
    }

    // getPresenter
    public com.google.gwt.activity.shared.Activity getPresenter(AbstractBasePlace place) {
        return place.createPresenter(this);
    }
}
