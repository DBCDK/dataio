package dk.dbc.dataio.gui.util;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreateView;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowView;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentCreateEditView;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowView;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreateView;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowView;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowView;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkCreateEditView;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowView;
import dk.dbc.dataio.gui.client.pages.submittermodify.View;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowView;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
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

    // Menu Structure
    MenuItem getMenuStructure();

    // Views
    FlowCreateView getFlowCreateView();
    FlowComponentCreateEditView getFlowComponentCreateEditView();
    View getSubmitterCreateView();
    View getSubmitterEditView();
    FlowbinderCreateView getFlowbinderCreateView();
    SinkCreateEditView getSinkCreateEditView();
    FlowComponentsShowView getFlowComponentsShowView();
    FlowsShowView getFlowsShowView();
    SubmittersShowView getSubmittersShowView();
    JobsShowView getJobsShowView();
    SinksShowView getSinksShowView();
    FlowBindersShowView getFlowBindersShowView();
}
