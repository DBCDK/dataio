package dk.dbc.dataio.gui.util;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.pages.flow.show.FlowsShowView;
import dk.dbc.dataio.gui.client.pages.flowbinder.show.FlowBindersShowView;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentCreateEditView;
import dk.dbc.dataio.gui.client.pages.flowcomponent.show.FlowComponentsShowView;
import dk.dbc.dataio.gui.client.pages.job.show.JobsShowView;
import dk.dbc.dataio.gui.client.pages.sink.show.SinksShowView;
import dk.dbc.dataio.gui.client.pages.submitter.show.SubmittersShowView;
import dk.dbc.dataio.gui.client.places.AppPlaceHistoryMapper;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.client.views.MenuItem;

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

    // Menu Structure
    MenuItem getMenuStructure();

    // Views
    dk.dbc.dataio.gui.client.pages.flow.modify.View getFlowCreateView();
    dk.dbc.dataio.gui.client.pages.flow.modify.View getFlowEditView();
    FlowComponentCreateEditView getFlowComponentCreateEditView();
    dk.dbc.dataio.gui.client.pages.submitter.modify.View getSubmitterCreateView();
    dk.dbc.dataio.gui.client.pages.submitter.modify.View getSubmitterEditView();
    dk.dbc.dataio.gui.client.pages.flowbinder.modify.View getFlowbinderCreateView();
    dk.dbc.dataio.gui.client.pages.sink.modify.View getSinkCreateView();
    dk.dbc.dataio.gui.client.pages.sink.modify.View getSinkEditView();
    FlowComponentsShowView getFlowComponentsShowView();
    FlowsShowView getFlowsShowView();
    SubmittersShowView getSubmittersShowView();
    JobsShowView getJobsShowView();
    SinksShowView getSinksShowView();
    dk.dbc.dataio.gui.client.pages.javascriptlog.View getJavaScriptLogView();
    FlowBindersShowView getFlowBindersShowView();
    dk.dbc.dataio.gui.client.pages.faileditems.View getFaileditemsView();

    AppPlaceHistoryMapper getHistoryMapper();
}
