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

package dk.dbc.dataio.gui.client.pages.sink.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.sink.modify.EditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This class represents the show sinks presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {
    private ClientFactory clientFactory;
    private View view;
    private FlowStoreProxyAsync flowStoreProxy;
    private final PlaceController placeController;


    /**
     * Default constructor
     *
     * @param clientFactory The client factory to be used
     */
    public PresenterImpl(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        placeController = clientFactory.getPlaceController();
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
        view = clientFactory.getSinksShowView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        fetchSinks();
    }


    /**
     * This method opens a new view, for editing the sink in question
     * @param model The model for the sink to edit
     */
    @Override
    public void editSink(SinkModel model) {
        placeController.goTo(new EditPlace(model));
    }

    /**
     * This method opens a new view, for creating a new sink
     */
    @Override
    public void createSink() {
        view.selectionModel.clear();
        placeController.goTo(new CreatePlace());
    }

    /*
     * Private methods
     */

    /**
     * This method fetches all sinks, and sends them to the view
     */
    private void fetchSinks() {
        flowStoreProxy.findAllSinks(new FetchSinksCallback());
    }

    /**
     * This method deciphers if a sink has been added, updated or deleted.
     * The view and selection model are updated accordingly
     *
     * @param models the list of sinks returned from flow store proxy
     */
    private void setSinksAndDecipherSelection(Set<SinkModel> dataProviderSet, List<SinkModel> models) {
        if (dataProviderSet.size() > models.size() || dataProviderSet.size() == 0) {
            view.selectionModel.clear();
            view.setSinks(models);
        } else {
            for (SinkModel current : models) {
                if (!dataProviderSet.contains(current)) {
                    view.setSinks(models);
                    view.selectionModel.setSelected(current, true);
                    break;
                }
            }
        }
    }

    /*
     * Private classes
     */

    /**
     * This class is the callback class for the findAllSinks method in the Flow Store
     */
    protected class FetchSinksCallback extends FilteredAsyncCallback<List<SinkModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, clientFactory.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<SinkModel> models) {
            setSinksAndDecipherSelection(new HashSet<SinkModel>(view.dataProvider.getList()), models);
        }
    }

}
