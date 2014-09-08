package dk.dbc.dataio.gui.util;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.pages.flowbinder.flowbindercreate.FlowbinderCreateView;
import dk.dbc.dataio.gui.client.pages.flowbinder.flowbindersshow.FlowBindersShowView;
import dk.dbc.dataio.gui.client.pages.flowcomponent.flowcomponentcreateedit.FlowComponentCreateEditView;
import dk.dbc.dataio.gui.client.pages.flowcomponent.flowcomponentsshow.FlowComponentsShowView;
import dk.dbc.dataio.gui.client.pages.flow.flowcreate.FlowCreateView;
import dk.dbc.dataio.gui.client.pages.flow.flowsshow.FlowsShowView;
import dk.dbc.dataio.gui.client.pages.job.jobsshow.JobsShowView;
import dk.dbc.dataio.gui.client.pages.sink.sinksshow.SinksShowView;
import dk.dbc.dataio.gui.client.pages.submitter.submittermodify.View;
import dk.dbc.dataio.gui.client.pages.submitter.submittersshow.SubmittersShowView;
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
    dk.dbc.dataio.gui.client.pages.sink.sinkmodify.View getSinkCreateView();
    dk.dbc.dataio.gui.client.pages.sink.sinkmodify.View getSinkEditView();
    FlowComponentsShowView getFlowComponentsShowView();
    FlowsShowView getFlowsShowView();
    SubmittersShowView getSubmittersShowView();
    JobsShowView getJobsShowView();
    SinksShowView getSinksShowView();
    FlowBindersShowView getFlowBindersShowView();
}
