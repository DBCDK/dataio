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

package dk.dbc.dataio.gui.client.pages.sink.status;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.pages.job.show.ShowTestJobsPlace;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.Arrays;
import java.util.List;


/**
 * This class represents the show sinks presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    private PlaceController placeController;
    private String header;

    /**
     * Default constructor
     *
     * @param placeController The Placecontroller
     * @param header breadcrumb Header text
     */
    public PresenterImpl(PlaceController placeController, String header) {
        this.placeController = placeController;
        this.header = header;
    }


    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        getView().setHeader(this.header);
        getView().setPresenter(this);
        containerWidget.setWidget(getView().asWidget());
        fetchSinkStatus();
    }


    /*
     * Public interface methods
     */

    public void showJobsFilteredBySink(long sinkId) {
        ShowTestJobsPlace showTestJobsPlace = ShowTestJobsPlace.getInstance();
        showTestJobsPlace.setToken("SuppressSubmitterJobFilter&SinkJobFilter=" + sinkId + "&ShowEarliestActive");
        GWT.log("Place goto: SuppressSubmitterJobFilter&SinkJobFilter=" + sinkId + "&ShowEarliestActive");
        placeController.goTo(showTestJobsPlace);
    }


    /*
     * Private methods
     */

    /**
     * This method fetches all sinks, and sends them to the view
     */
    private void fetchSinkStatus() {
        // Temporary fix (until backend is ready)
        List<SinkStatusTable.SinkStatusModel> sinkStatus = Arrays.asList(
                new SinkStatusTable.SinkStatusModel(  54, "Dummy sink", "Dummy sink", 0, 0, Format.parseLongDateAsLong("2016-09-11 11:22:53")),
                new SinkStatusTable.SinkStatusModel(6601, "Dummy sink", "Tracer bullit sink", 0, 0, Format.parseLongDateAsLong("2016-10-10 11:11:11")),
                new SinkStatusTable.SinkStatusModel(1551, "ES sink", "Basis22", 2, 4, Format.parseLongDateAsLong("2016-10-11 11:20:53")),
                new SinkStatusTable.SinkStatusModel(5701, "ES sink", "Danbib3", 0, 0, Format.parseLongDateAsLong("2016-10-11 11:22:53")),
                new SinkStatusTable.SinkStatusModel( 752, "Hive sink", "Cisterne sink", 34, 56, Format.parseLongDateAsLong("2016-10-11 11:22:52")),
                new SinkStatusTable.SinkStatusModel(   8, "Hive sink", "Boblebad sink", 32, 54, Format.parseLongDateAsLong("2016-10-11 11:20:01")),
                new SinkStatusTable.SinkStatusModel(1651, "Update sink", "Cisterne Update sink", 1, 56023, Format.parseLongDateAsLong("2016-10-11 11:22:53")),
                new SinkStatusTable.SinkStatusModel(5401, "IMS sink", "IMS cisterne sink", 7, 8, Format.parseLongDateAsLong("2016-10-11 11:22:48"))
        );
        getView().setSinkStatus(sinkStatus);
    }

    private View getView() {
        return viewInjector.getView();
    }

}
