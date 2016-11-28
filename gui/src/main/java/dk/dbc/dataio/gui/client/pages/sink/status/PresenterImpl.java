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
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.pages.job.show.ShowJobsPlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.List;


/**
 * This class represents the show sinks presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
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
        ShowJobsPlace showJobsPlace = ShowJobsPlace.getInstance();
        showJobsPlace.setToken("SuppressSubmitterJobFilter&SinkJobFilter=" + sinkId + "&ShowEarliestActive");
        placeController.goTo(showJobsPlace);
    }


    /*
     * Private methods
     */

    /**
     * This method fetches all sinks, and sends them to the view
     */
    private void fetchSinkStatus() {
        commonInjector.getJobStoreProxyAsync().getSinkStatusModels(new GetSinkStatusListFilteredAsyncCallback());
    }

    class GetSinkStatusListFilteredAsyncCallback extends FilteredAsyncCallback<List<SinkStatusTable.SinkStatusModel>> {
        @Override
        public void onFilteredFailure(Throwable throwable) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromJobStoreProxy(throwable, commonInjector.getProxyErrorTexts(), null));
        }
        @Override
        public void onSuccess(List<SinkStatusTable.SinkStatusModel> sinkStatusSnapshots) {
            getView().setSinkStatus(sinkStatusSnapshots);
        }
    }

    private View getView() {
        return viewInjector.getView();
    }

}
