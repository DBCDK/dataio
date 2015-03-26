package dk.dbc.dataio.gui.util;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.places.AppPlaceHistoryMapper;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;

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
    Activity getPresenter(Place place);

    // Proxies
    FlowStoreProxyAsync getFlowStoreProxyAsync();
    JavaScriptProjectFetcherAsync getJavaScriptProjectFetcherAsync();
    SinkServiceProxyAsync getSinkServiceProxyAsync();
    JobStoreProxyAsync getJobStoreProxyAsync();
    LogStoreProxyAsync getLogStoreProxyAsync();

    // Views
    dk.dbc.dataio.gui.client.pages.flow.modify.View getFlowCreateView();
    dk.dbc.dataio.gui.client.pages.flow.modify.View getFlowEditView();
    dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View getFlowComponentCreateView();
    dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View getFlowComponentEditView();
    dk.dbc.dataio.gui.client.pages.submitter.modify.View getSubmitterCreateView();
    dk.dbc.dataio.gui.client.pages.submitter.modify.View getSubmitterEditView();
    dk.dbc.dataio.gui.client.pages.flowbinder.modify.View getFlowBinderCreateView();
    dk.dbc.dataio.gui.client.pages.flowbinder.modify.View getFlowBinderEditView();
    dk.dbc.dataio.gui.client.pages.sink.modify.View getSinkCreateView();
    dk.dbc.dataio.gui.client.pages.sink.modify.View getSinkEditView();
    dk.dbc.dataio.gui.client.pages.flowcomponent.show.View getFlowComponentsShowView();
    dk.dbc.dataio.gui.client.pages.flow.show.View getFlowsShowView();
    dk.dbc.dataio.gui.client.pages.submitter.show.View getSubmittersShowView();
    dk.dbc.dataio.gui.client.pages.job.show.View getJobsShowView();
    dk.dbc.dataio.gui.client.pages.sink.show.View getSinksShowView();
    dk.dbc.dataio.gui.client.pages.javascriptlog.View getJavaScriptLogView();
    dk.dbc.dataio.gui.client.pages.flowbinder.show.View getFlowBindersShowView();
    dk.dbc.dataio.gui.client.pages.faileditems.View getFaileditemsView();
    dk.dbc.dataio.gui.client.pages.item.show.View getItemsShowView();

    AppPlaceHistoryMapper getHistoryMapper();

}
