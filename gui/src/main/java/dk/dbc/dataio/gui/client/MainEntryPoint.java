package dk.dbc.dataio.gui.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.activities.AppActivityMapper;
import dk.dbc.dataio.gui.client.places.AppPlaceHistoryMapper;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.gui.client.views.MainPanel;

public class MainEntryPoint implements EntryPoint {
    final ClientFactory clientFactory = GWT.create(ClientFactory.class);
    final private MainPanel appPanel = new MainPanel(clientFactory);

    @Override
    public void onModuleLoad() {
        EventBus eventBus = clientFactory.getEventBus();
        PlaceController placeController = clientFactory.getPlaceController();

        // Start ActivityManager for the main widget with our ActivityMapper
        ActivityMapper activityMapper = new AppActivityMapper(clientFactory);
        ActivityManager activityManager = new ActivityManager(activityMapper, eventBus);
        activityManager.setDisplay(appPanel.contentPanel);

        // Start PlaceHistoryHandler with our PlaceHistoryMapper
        AppPlaceHistoryMapper historyMapper= GWT.create(AppPlaceHistoryMapper.class);
        PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
        historyHandler.register(placeController, eventBus, null);
        historyHandler.handleCurrentHistory();

        // Show the root panel
        RootLayoutPanel.get().add(appPanel);
    }

}
