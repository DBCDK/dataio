package dk.dbc.dataio.gui.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.activities.AppActivityMapper;
import dk.dbc.dataio.gui.client.exceptions.DioUncaughtExceptionHandler;
import dk.dbc.dataio.gui.client.pages.job.show.Place;
import dk.dbc.dataio.gui.client.places.AppPlaceHistoryMapper;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 *
 * Main Entry Point for the GWT GUI
 *
 */
public class MainEntryPoint implements EntryPoint {
    final ClientFactory clientFactory = GWT.create(ClientFactory.class);
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
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
          @Override
          public void execute() {
            deferredOnModuleLoad();
          }
        });
    }

    /**
     * The body part of the onModuleLoad
     * This code is executed after the UncaughtExceptionHandler has been set, so
     * any uncaught exceptions will be catched.
     *
     * This is the main entry point for the GWT application.
     *
     */
    private void deferredOnModuleLoad() {
        EventBus eventBus = clientFactory.getEventBus();
        PlaceController placeController = clientFactory.getPlaceController();

        // Start ActivityManager for the main widget with our ActivityMapper
        ActivityMapper activityMapper = new AppActivityMapper(clientFactory);
        ActivityManager activityManager = new ActivityManager(activityMapper, eventBus);
        activityManager.setDisplay(appPanel.applicationPanel);

        // Start PlaceHistoryHandler with our PlaceHistoryMapper
        AppPlaceHistoryMapper historyMapper = clientFactory.getHistoryMapper();
        PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
        historyHandler.register(placeController, eventBus, new Place());
        historyHandler.handleCurrentHistory();

        // Show the root panel
        RootLayoutPanel.get().add(appPanel);
    }
}
