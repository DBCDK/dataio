package dk.dbc.dataio.gui.util;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreateGenericView;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowGenericView;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentCreateEditGenericView;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowGenericView;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreateGenericView;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowGenericView;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowGenericView;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkCreateEditGenericView;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowGenericView;
import dk.dbc.dataio.gui.client.pages.submittercreate.SubmitterCreateGenericView;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowGenericView;
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
    Activity getActivity(Place place);

    // Proxies
    FlowStoreProxyAsync getFlowStoreProxyAsync();
    JavaScriptProjectFetcherAsync getJavaScriptProjectFetcherAsync();
    SinkServiceProxyAsync getSinkServiceProxyAsync();
    JobStoreProxyAsync getJobStoreProxyAsync();

    // Menu Structure
    MenuItem getMenuStructure();

    // Views
    FlowCreateGenericView getFlowCreateView();
    FlowComponentCreateEditGenericView getFlowComponentCreateEditView();
    SubmitterCreateGenericView getSubmitterCreateView();
    FlowbinderCreateGenericView getFlowbinderCreateView();
    SinkCreateEditGenericView getSinkCreateEditView();
    FlowComponentsShowGenericView getFlowComponentsShowView();
    FlowsShowGenericView getFlowsShowView();
    SubmittersShowGenericView getSubmittersShowView();
    JobsShowGenericView getJobsShowView();
    SinksShowGenericView getSinksShowView();
    FlowBindersShowGenericView getFlowBindersShowView();
    //HarvestersShowView getHarvestersShowView();

}
