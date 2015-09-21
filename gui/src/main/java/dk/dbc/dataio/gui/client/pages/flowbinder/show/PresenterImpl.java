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

package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This class represents the show flow binders presenter implementation
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
        view = clientFactory.getFlowBindersShowView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        fetchFlowBinders();
    }


    /**
     * This method opens a new view, for editing the flowbinder in question
     * @param model The model for the flowbinder to edit
     */
    @Override
    public void editFlowBinder(FlowBinderModel model) {
        placeController.goTo(new EditPlace(model));
    }

    /**
     * This method opens a new view, for creating a new Flowbinder.
     */
    @Override
    public void createFlowBinder() {
        view.selectionModel.clear();
        placeController.goTo(new CreatePlace());
    }

    /*
     * Local methods
     */

    /**
     * This method fetches all flowbinders, and sends them to the view
     */
    private void fetchFlowBinders() {
        flowStoreProxy.findAllFlowBinders(new FetchFlowBindersCallback());
    }

    /**
     * This method deciphers if a flow binder has been added, updated or deleted.
     * The view and selection model are updated accordingly
     *
     * @param models the list of flow binders returned from flow store proxy
     */
    private void setFlowBindersAndDecipherSelection(Set<FlowBinderModel> dataProviderSet, List<FlowBinderModel> models) {
        if (dataProviderSet.size() > models.size() || dataProviderSet.size() == 0) {
            view.selectionModel.clear();
            view.setFlowBinders(models);
        } else {
            for (FlowBinderModel current : models) {
                if (!dataProviderSet.contains(current)) {
                    view.setFlowBinders(models);
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
     * This class is the callback class for the findAllFlowBinders method in the Flow Store
     */
    protected class FetchFlowBindersCallback extends FilteredAsyncCallback<List<FlowBinderModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, clientFactory.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }
        @Override
        public void onSuccess(List<FlowBinderModel> models) {
            setFlowBindersAndDecipherSelection(new HashSet<FlowBinderModel>(view.dataProvider.getList()), models);
        }
    }

}
