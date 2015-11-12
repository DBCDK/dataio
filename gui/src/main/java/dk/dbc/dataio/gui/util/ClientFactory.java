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

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.places.AppPlaceHistoryMapper;
import dk.dbc.dataio.gui.client.places.DataioPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.client.resources.Resources;

/**
 * Provides access to common objects across the application including:
 * - views as singleton objects, which improves performance since views contain DOM calls which are expensive.
 * - shared event bus.
 * - any RPC proxies.
 */
public interface ClientFactory {
    // Event Bus
    EventBus getEventBus();

    // Place Controller
    PlaceController getPlaceController();
    Activity getPresenter(DataioPlace place);

    // Proxies
    FlowStoreProxyAsync getFlowStoreProxyAsync();
    JavaScriptProjectFetcherAsync getJavaScriptProjectFetcherAsync();
    JobStoreProxyAsync getJobStoreProxyAsync();
    LogStoreProxyAsync getLogStoreProxyAsync();

    // Views
    dk.dbc.dataio.gui.client.pages.flow.modify.CreateView getFlowCreateView();
    dk.dbc.dataio.gui.client.pages.flow.modify.EditView getFlowEditView();
    dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreateView getFlowComponentCreateView();
    dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditView getFlowComponentEditView();
    dk.dbc.dataio.gui.client.pages.job.modify.EditView getJobEditView();
    dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreateView getFlowBinderCreateView();
    dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditView getFlowBinderEditView();
    dk.dbc.dataio.gui.client.pages.flowcomponent.show.View getFlowComponentsShowView();
    dk.dbc.dataio.gui.client.pages.flow.show.View getFlowsShowView();
    dk.dbc.dataio.gui.client.pages.job.show.View getJobsShowView();
    dk.dbc.dataio.gui.client.pages.job.show.View getTestJobsShowView();
    dk.dbc.dataio.gui.client.pages.flowbinder.show.View getFlowBindersShowView();
    dk.dbc.dataio.gui.client.pages.item.show.View getItemsShowView();

    // Menu text
    dk.dbc.dataio.gui.client.pages.navigation.Texts getMenuTexts();

    // Texts
    dk.dbc.dataio.gui.client.pages.submitter.modify.Texts getSubmitterModifyTexts();
    dk.dbc.dataio.gui.client.pages.job.modify.Texts getJobModifyTexts();
    dk.dbc.dataio.gui.client.pages.flow.modify.Texts getFlowModifyTexts();
    dk.dbc.dataio.gui.client.pages.flowcomponent.modify.Texts getFlowComponentModifyTexts();
    dk.dbc.dataio.gui.client.pages.flowbinder.modify.Texts getFlowBinderModifyTexts();
    dk.dbc.dataio.gui.client.pages.item.show.Texts getItemsShowTexts();
    dk.dbc.dataio.gui.client.pages.flow.show.Texts getFlowsShowTexts();
    dk.dbc.dataio.gui.client.pages.flowcomponent.show.Texts getFlowComponentsShowTexts();
    dk.dbc.dataio.gui.client.pages.flowbinder.show.Texts getFlowBindersShowTexts();
    dk.dbc.dataio.gui.client.pages.job.show.Texts getJobsShowTexts();
    dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts getProxyErrorTexts();

    // Resources
    Resources getImageResources();


    AppPlaceHistoryMapper getHistoryMapper();

}
