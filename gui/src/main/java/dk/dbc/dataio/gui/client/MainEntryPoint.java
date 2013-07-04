package dk.dbc.dataio.gui.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.RootPanel;
import dk.dbc.dataio.gui.places.FlowEditPlace;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.gui.views.MainPanel;
import dk.dbc.dataio.gui.activities.FlowEditActivity;

public class MainEntryPoint implements EntryPoint {
    final ClientFactory clientFactory;
    final private MainPanel appPanel;
    final private Place defaultPlace;

    public MainEntryPoint() {
        clientFactory = GWT.create(ClientFactory.class);
        appPanel = new MainPanel(clientFactory);
        defaultPlace = new FlowEditPlace("Hallo der...");
    }

    @Override
    public void onModuleLoad() {
        //Indtil Activity Manageren starter den:
        FlowEditActivity flowEditActivity = new FlowEditActivity((FlowEditPlace) defaultPlace, clientFactory);
        //flowEditActivity.start(appPanel, (EventBus) clientFactory.getEventBus());
        
//        EventBus eventBus = clientFactory.getEventBus();
//        PlaceController placeController = clientFactory.getPlaceController();
//
//        // Start ActivityManager for the main widget with our ActivityMapper
//        ActivityMapper activityMapper = new AppActivityMapper(clientFactory);
//        ActivityManager activityManager = new ActivityManager(activityMapper, eventBus);
//        activityManager.setDisplay(appPanel);
//        
//        // Start PlaceHistoryHandler with our PlaceHistoryMapper
//        AppPlaceHistoryMapper historyMapper= GWT.create(AppPlaceHistoryMapper.class);
//        PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
//        historyHandler.register(placeController, eventBus, defaultPlace);

        RootPanel.get().add(appPanel);
        // Goes to the place represented on URL else default place
//        historyHandler.handleCurrentHistory();
    }

}
