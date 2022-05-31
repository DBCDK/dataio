package dk.dbc.dataio.gui.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.activities.AppActivityMapper;
import dk.dbc.dataio.gui.client.exceptions.DioUncaughtExceptionHandler;
import dk.dbc.dataio.gui.client.pages.job.show.ShowJobsPlace;
import dk.dbc.dataio.gui.client.places.AppPlaceHistoryMapper;
import dk.dbc.dataio.gui.client.places.DioPlaceHistoryHandler;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;

/**
 * Main Entry Point for the GWT GUI
 */
public class MainEntryPoint implements EntryPoint {
    final private ClientFactory clientFactory = ClientFactoryImpl.getInstance();
    final private MainPanel appPanel = new MainPanel(clientFactory);

    /**
     * onModuleLoad
     */
    @Override
    public void onModuleLoad() {
        // Setup an exception handler for Uncaught Exceptions
        GWT.setUncaughtExceptionHandler(new DioUncaughtExceptionHandler());
        // The new Uncaught Exception Handler will take effect when onModuleLoad has completed.
        // Therefore, we need to defer the remaining part of the onModuleLoad
        // Please refer to: http://www.summa-tech.com/blog/2012/06/11/7-tips-for-exception-handling-in-gwt/
        Scheduler.get().scheduleDeferred(this::deferredOnModuleLoad);
    }

    /**
     * The body part of the onModuleLoad
     * This code is executed after the UncaughtExceptionHandler has been set, so
     * any uncaught exceptions will be catched.
     * <p>
     * This is the main entry point for the GWT application.
     */
    private void deferredOnModuleLoad() {
        EventBus eventBus = clientFactory.getEventBus();
        PlaceController placeController = clientFactory.getPlaceController();
        Resources.INSTANCE.css().ensureInjected();


        // Start ActivityManager for the main widget with our ActivityMapper
        ActivityMapper activityMapper = new AppActivityMapper(clientFactory);
        ActivityManager activityManager = new ActivityManager(activityMapper, eventBus);
        activityManager.setDisplay(appPanel.applicationPanel);

        // Start PlaceHistoryHandler with our PlaceHistoryMapper
        AppPlaceHistoryMapper historyMapper = clientFactory.getHistoryMapper();
        DioPlaceHistoryHandler historyHandler = new DioPlaceHistoryHandler(historyMapper);
        historyHandler.register(placeController, eventBus, new ShowJobsPlace());
        historyHandler.handleCurrentHistory();

        // Set the title of the Browser Window
        if (Document.get() != null) {
            Document.get().setTitle("DBC - Data I/O");
        }

        // Show the root panel
        RootLayoutPanel.get().add(appPanel);

//        eventBusDebug();
    }

    @SuppressWarnings("unused")
    private void eventBusDebug() {
        EventBus eventBus = clientFactory.getEventBus();
        eventBus.addHandler(PlaceChangeEvent.TYPE, event -> GWT.log("++++> PlaceChangeEvent - New Place: -> " + event.getNewPlace()));
    }

}
