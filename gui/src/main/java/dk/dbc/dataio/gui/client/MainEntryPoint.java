/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.activities.AppActivityMapper;
import dk.dbc.dataio.gui.client.exceptions.DioUncaughtExceptionHandler;
import dk.dbc.dataio.gui.client.pages.job.show.ShowJobsPlace;
import dk.dbc.dataio.gui.client.places.AppPlaceHistoryMapper;
import dk.dbc.dataio.gui.client.resources.Resources;
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
        Scheduler.get().scheduleDeferred(this::deferredOnModuleLoad);
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
        Resources.INSTANCE.css().ensureInjected();


        // Start ActivityManager for the main widget with our ActivityMapper
        ActivityMapper activityMapper = new AppActivityMapper(clientFactory);
        ActivityManager activityManager = new ActivityManager(activityMapper, eventBus);
        activityManager.setDisplay(appPanel.applicationPanel);

        // Start PlaceHistoryHandler with our PlaceHistoryMapper
        AppPlaceHistoryMapper historyMapper = clientFactory.getHistoryMapper();
        PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
        historyHandler.register(placeController, eventBus, ShowJobsPlace.getInstance());
        historyHandler.handleCurrentHistory();

        // Set the title of the Browser Window
        if (Document.get() != null) {
            Document.get().setTitle("DBC - Data I/O");
        }

        // Show the root panel
        RootLayoutPanel.get().add(appPanel);

//        extra();
    }

    private void extra() {
        EventBus eventBus = clientFactory.getEventBus();
        eventBus.addHandler(PlaceChangeEvent.TYPE, event -> GWT.log("++++> PlaceChangeEvent - New Place: -> " + event.getNewPlace()));
    }

}
