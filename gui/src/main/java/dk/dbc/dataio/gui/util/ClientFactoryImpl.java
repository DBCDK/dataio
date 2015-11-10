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

package dk.dbc.dataio.gui.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import dk.dbc.dataio.gui.client.places.AppPlaceHistoryMapper;
import dk.dbc.dataio.gui.client.places.DataioPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxy;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxy;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.client.resources.Resources;

public class ClientFactoryImpl implements ClientFactory {

    // Menu texts constants declarations
    private final static dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts proxyErrorTexts = GWT.create(dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts.class);
    private final static dk.dbc.dataio.gui.client.pages.navigation.Texts menuTexts = GWT.create(dk.dbc.dataio.gui.client.pages.navigation.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.submitter.show.Texts submittersShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.submitter.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flow.show.Texts flowsShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flow.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowbinder.show.Texts flowBindersShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowbinder.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.sink.show.Texts sinksShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.sink.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.job.show.Texts jobsShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.job.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowcomponent.show.Texts flowComponentsShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowcomponent.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flow.modify.Texts flowModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flow.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowcomponent.modify.Texts flowComponentModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowcomponent.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowbinder.modify.Texts flowBinderModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowbinder.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.sink.modify.Texts sinkModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.sink.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.submitter.modify.Texts submitterModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.submitter.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.job.modify.Texts jobModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.job.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.item.show.Texts itemsShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.item.show.Texts.class);

    // Image Resources
    private final static Resources RESOURCES = GWT.create(Resources.class);

    // Event Bus
    private final EventBus eventBus = new SimpleEventBus();

    // Place Controller
    private final PlaceController placeController = new PlaceController(eventBus);

    // History Mapper
    private final AppPlaceHistoryMapper historyMapper = GWT.create(AppPlaceHistoryMapper.class);

    // Proxies
    private final FlowStoreProxyAsync flowStoreProxyAsync = FlowStoreProxy.Factory.getAsyncInstance();
    private final JavaScriptProjectFetcherAsync javaScriptProjectFetcher = JavaScriptProjectFetcher.Factory.getAsyncInstance();
    private final SinkServiceProxyAsync sinkServiceProxyAsync = SinkServiceProxy.Factory.getAsyncInstance();
    private final JobStoreProxyAsync jobStoreProxyAsync = JobStoreProxy.Factory.getAsyncInstance();
    private final LogStoreProxyAsync logStoreProxyAsync = LogStoreProxy.Factory.getAsyncInstance();

    // Views
    private final dk.dbc.dataio.gui.client.pages.flow.modify.CreateView flowCreateView = new dk.dbc.dataio.gui.client.pages.flow.modify.CreateView(this);
    private final dk.dbc.dataio.gui.client.pages.flow.modify.EditView flowEditView = new dk.dbc.dataio.gui.client.pages.flow.modify.EditView(this);
    private final dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreateView flowComponentCreateView = new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreateView(this);
    private final dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditView flowComponentEditView = new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditView(this);
    private final dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreateView flowBinderCreateView = new dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreateView(this);
    private final dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditView flowBinderEditView = new dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditView(this);
    private final dk.dbc.dataio.gui.client.pages.submitter.modify.CreateView submitterCreateView = new dk.dbc.dataio.gui.client.pages.submitter.modify.CreateView(this);
    private final dk.dbc.dataio.gui.client.pages.submitter.modify.EditView submitterEditView = new dk.dbc.dataio.gui.client.pages.submitter.modify.EditView(this);
    private final dk.dbc.dataio.gui.client.pages.job.modify.EditView jobEditView = new dk.dbc.dataio.gui.client.pages.job.modify.EditView(this);

    private final dk.dbc.dataio.gui.client.pages.flowcomponent.show.View flowComponentsShowView = new dk.dbc.dataio.gui.client.pages.flowcomponent.show.View(this);
    private final dk.dbc.dataio.gui.client.pages.flow.show.View flowsShowView = new dk.dbc.dataio.gui.client.pages.flow.show.View(this);
    private final dk.dbc.dataio.gui.client.pages.submitter.show.View submittersShowView = new dk.dbc.dataio.gui.client.pages.submitter.show.View(this);
    private final dk.dbc.dataio.gui.client.pages.job.show.View jobsShowView = new dk.dbc.dataio.gui.client.pages.job.show.JobsView(this);
    private final dk.dbc.dataio.gui.client.pages.job.show.View testJobsShowView = new dk.dbc.dataio.gui.client.pages.job.show.TestJobsView(this);
    private final dk.dbc.dataio.gui.client.pages.sink.show.View sinksShowView = new dk.dbc.dataio.gui.client.pages.sink.show.View(this);
    private final dk.dbc.dataio.gui.client.pages.flowbinder.show.View flowBindersShowView = new dk.dbc.dataio.gui.client.pages.flowbinder.show.View(this);
    private final dk.dbc.dataio.gui.client.pages.item.show.View itemsShowView = new dk.dbc.dataio.gui.client.pages.item.show.View(this);

    public ClientFactoryImpl() {
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
    public com.google.gwt.activity.shared.Activity getPresenter(DataioPlace place) {
        return place.createPresenter(this);
    }

    // Proxies
    @Override
    public FlowStoreProxyAsync getFlowStoreProxyAsync() {
        return flowStoreProxyAsync;
    }

    @Override
    public JavaScriptProjectFetcherAsync getJavaScriptProjectFetcherAsync() {
        return javaScriptProjectFetcher;
    }

    @Override
    public SinkServiceProxyAsync getSinkServiceProxyAsync() {
        return sinkServiceProxyAsync;
    }

    @Override
    public JobStoreProxyAsync getJobStoreProxyAsync() {
        return jobStoreProxyAsync;
    }

    @Override
    public LogStoreProxyAsync getLogStoreProxyAsync() {
        return logStoreProxyAsync;
    }


    // Views
    @Override
    public dk.dbc.dataio.gui.client.pages.flow.modify.CreateView getFlowCreateView() {
        return flowCreateView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flow.modify.EditView getFlowEditView() {
        return flowEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreateView getFlowComponentCreateView() {
        return flowComponentCreateView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditView getFlowComponentEditView() {
        return flowComponentEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.submitter.modify.CreateView getSubmitterCreateView() {
        return submitterCreateView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.submitter.modify.EditView getSubmitterEditView() {
        return submitterEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.job.modify.EditView getJobEditView() {
        return jobEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreateView getFlowBinderCreateView() {
        return flowBinderCreateView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditView getFlowBinderEditView() {
        return flowBinderEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowcomponent.show.View getFlowComponentsShowView() {
        return flowComponentsShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flow.show.View getFlowsShowView() {
        return flowsShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.submitter.show.View getSubmittersShowView() {
        return submittersShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.job.show.View getJobsShowView() {
        return jobsShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.job.show.View getTestJobsShowView() {
        return testJobsShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.sink.show.View getSinksShowView() {
        return sinksShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowbinder.show.View getFlowBindersShowView() {
        return flowBindersShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.item.show.View getItemsShowView() {
        return itemsShowView;
    }


    // Menu text
    @Override
    public dk.dbc.dataio.gui.client.pages.navigation.Texts getMenuTexts() {
        return menuTexts;
    }


    // Texts
    @Override
    public dk.dbc.dataio.gui.client.pages.submitter.modify.Texts getSubmitterModifyTexts() {
        return submitterModifyTexts;
    }
    @Override
    public dk.dbc.dataio.gui.client.pages.job.modify.Texts getJobModifyTexts() {
        return jobModifyTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flow.modify.Texts getFlowModifyTexts() {
        return flowModifyTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowcomponent.modify.Texts getFlowComponentModifyTexts() {
        return flowComponentModifyTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowbinder.modify.Texts getFlowBinderModifyTexts() {
        return flowBinderModifyTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.sink.modify.Texts getSinkModifyTexts() {
        return sinkModifyTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.item.show.Texts getItemsShowTexts() {
        return itemsShowTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.submitter.show.Texts getSubmittersShowTexts() {
        return submittersShowTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flow.show.Texts getFlowsShowTexts() {
        return flowsShowTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowcomponent.show.Texts getFlowComponentsShowTexts() {
        return flowComponentsShowTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowbinder.show.Texts getFlowBindersShowTexts() {
        return flowBindersShowTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.sink.show.Texts getSinksShowTexts() {
        return sinksShowTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.job.show.Texts getJobsShowTexts() {
        return jobsShowTexts;
    }

    @Override
    public dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts getProxyErrorTexts() {
        return proxyErrorTexts;
    }


    // Image resources
    @Override
    public Resources getImageResources() {
        return RESOURCES;
    }

}