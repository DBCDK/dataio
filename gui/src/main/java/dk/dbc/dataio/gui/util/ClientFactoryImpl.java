package dk.dbc.dataio.gui.util;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreateActivity;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreateConstants;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreateView;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreateViewImpl;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowActivity;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowConstants;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowPlace;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowView;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowViewImpl;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreate.FlowComponentCreateActivity;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreate.FlowComponentCreateConstants;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreate.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreate.FlowComponentCreateView;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreate.FlowComponentCreateViewImpl;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowActivity;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowConstants;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowView;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowViewImpl;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreateActivity;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreateConstants;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreateView;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreateViewImpl;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowActivity;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowConstants;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowPlace;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowView;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowViewImpl;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowActivity;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowConstants;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowPlace;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowView;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowViewImpl;
import dk.dbc.dataio.gui.client.pages.sinkcreate.SinkCreateActivity;
import dk.dbc.dataio.gui.client.pages.sinkcreate.SinkCreateConstants;
import dk.dbc.dataio.gui.client.pages.sinkcreate.SinkCreatePlace;
import dk.dbc.dataio.gui.client.pages.sinkcreate.SinkCreateView;
import dk.dbc.dataio.gui.client.pages.sinkcreate.SinkCreateViewImpl;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowActivity;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowConstants;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowPlace;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowView;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowViewImpl;
import dk.dbc.dataio.gui.client.pages.submittercreate.SubmitterCreateActivity;
import dk.dbc.dataio.gui.client.pages.submittercreate.SubmitterCreateConstants;
import dk.dbc.dataio.gui.client.pages.submittercreate.SubmitterCreatePlace;
import dk.dbc.dataio.gui.client.pages.submittercreate.SubmitterCreateView;
import dk.dbc.dataio.gui.client.pages.submittercreate.SubmitterCreateViewImpl;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowActivity;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowConstants;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowPlace;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowView;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowViewImpl;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxy;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.client.views.MenuItem;

public class ClientFactoryImpl implements ClientFactory {

    // Menu Item Id's
    public final static String GUIID_MENU_ITEM_SUBMITTERS_SHOW = "menuitemsubmittersshow";
    public final static String GUIID_MENU_ITEM_FLOWS_SHOW = "menuitemflowsshow";
    public final static String GUIID_MENU_ITEM_SINKS_SHOW = "menuitemsinksshow";
    public final static String GUIID_MENU_ITEM_JOBS_SHOW = "menuitemjobsshow";
    public final static String GUIID_MENU_ITEM_FLOW_COMPONENTS_SHOW = "menuitemflowcomponentsshow";
    public final static String GUIID_MENU_ITEM_FLOW_BINDERS_SHOW = "menuitemflowbindersshow";
    public final static String GUIID_MENU_ITEM_SUBMITTER_CREATE = "menuitemsubmittercreate";
    public final static String GUIID_MENU_ITEM_FLOW_CREATE = "menuitemflowcreate";
    public final static String GUIID_MENU_ITEM_FLOW_COMPONENT_CREATE = "menuitemflowcomponentcreate";
    public final static String GUIID_MENU_ITEM_FLOWBINDER_CREATE = "menuitemflowbindercreate";
    public final static String GUIID_MENU_ITEM_SINK_CREATE = "menuitemsinkcreate";

    // Menu texts constants declarations
    private final static SubmittersShowConstants submittersShowConstants = GWT.create(SubmittersShowConstants.class);
    private final static FlowsShowConstants flowsShowConstants = GWT.create(FlowsShowConstants.class);
    private final static FlowBindersShowConstants flowBindersShowConstants = GWT.create(FlowBindersShowConstants.class);
    private final static SinksShowConstants sinksShowConstants = GWT.create(SinksShowConstants.class);
    private final static JobsShowConstants jobsShowConstants = GWT.create(JobsShowConstants.class);
    private final static FlowComponentsShowConstants flowComponentsShowConstants = GWT.create(FlowComponentsShowConstants.class);
    private final static SubmitterCreateConstants submitterCreateConstants = GWT.create(SubmitterCreateConstants.class);
    private final static FlowCreateConstants flowCreateConstants = GWT.create(FlowCreateConstants.class);
    private final static FlowComponentCreateConstants flowComponentCreateConstants = GWT.create(FlowComponentCreateConstants.class);
    private final static FlowbinderCreateConstants flowbinderCreateConstants = GWT.create(FlowbinderCreateConstants.class);
    private final static SinkCreateConstants sinkCreateConstants = GWT.create(SinkCreateConstants.class);

    // Event Bus
    private final EventBus eventBus = new SimpleEventBus();

    // Place Controller
    private final PlaceController placeController = new PlaceController(eventBus);
    public final static Place NOWHERE = null;

    // Proxies
    private final FlowStoreProxyAsync flowStoreProxyAsync = FlowStoreProxy.Factory.getAsyncInstance();
    private final JavaScriptProjectFetcherAsync javaScriptProjectFetcher = JavaScriptProjectFetcher.Factory.getAsyncInstance();
    private final SinkServiceProxyAsync sinkServiceProxyAsync = SinkServiceProxy.Factory.getAsyncInstance();
    private final JobStoreProxyAsync jobStoreProxyAsync = JobStoreProxy.Factory.getAsyncInstance();

    // Menu Structure
    private final MenuItem menuStructure;

    // Views
    private final FlowCreateView flowCreateView = new FlowCreateViewImpl();
    private final FlowComponentCreateView flowComponentCreateView = new FlowComponentCreateViewImpl();
    private final SubmitterCreateView submitterCreateView = new SubmitterCreateViewImpl();
    private final FlowbinderCreateView flowbinderCreateView = new FlowbinderCreateViewImpl();
    private final SinkCreateView sinkCreateView = new SinkCreateViewImpl();
    private final FlowComponentsShowView flowComponentsShowView = new FlowComponentsShowViewImpl();
    private final FlowsShowView flowsShowView = new FlowsShowViewImpl();
    private final SubmittersShowView submittersShowView = new SubmittersShowViewImpl();
    private final JobsShowView jobsShowView = new JobsShowViewImpl();
    private final SinksShowView sinksShowView = new SinksShowViewImpl();
    private final FlowBindersShowView flowBindersShowView = new FlowBindersShowViewImpl();

    public ClientFactoryImpl() {
        // Submitters Main Menu
        MenuItem createSubmitter = new MenuItem(GUIID_MENU_ITEM_SUBMITTER_CREATE, submitterCreateConstants.menu_SubmitterCreation(), new SubmitterCreatePlace());
        MenuItem submittersMenu = new MenuItem(GUIID_MENU_ITEM_SUBMITTERS_SHOW, submittersShowConstants.menu_Submitters(), new SubmittersShowPlace(),
                                               createSubmitter);

        // Flows Main Menu
        MenuItem createFlow = new MenuItem(GUIID_MENU_ITEM_FLOW_CREATE, flowCreateConstants.menu_FlowCreation(), new FlowCreatePlace());
        MenuItem createFlowComponent = new MenuItem(GUIID_MENU_ITEM_FLOW_COMPONENT_CREATE, flowComponentCreateConstants.menu_FlowComponentCreation(), new FlowComponentCreatePlace());
        MenuItem showFlowComponents = new MenuItem(GUIID_MENU_ITEM_FLOW_COMPONENTS_SHOW, flowComponentsShowConstants.menu_FlowComponentsShow(), new FlowComponentsShowPlace());
        MenuItem createFlowBinder = new MenuItem(GUIID_MENU_ITEM_FLOWBINDER_CREATE, flowbinderCreateConstants.menu_FlowbinderCreation(), new FlowbinderCreatePlace());
        MenuItem showFlowBinders = new MenuItem(GUIID_MENU_ITEM_FLOW_BINDERS_SHOW, flowBindersShowConstants.menu_FlowBindersShow(), new FlowBindersShowPlace());
        MenuItem flowsMenu = new MenuItem(GUIID_MENU_ITEM_FLOWS_SHOW, flowsShowConstants.menu_Flows(), new FlowsShowPlace(),
                                          createFlow,
                                          createFlowComponent,
                                          showFlowComponents,
                                          createFlowBinder,
                                          showFlowBinders);

        // Sinks Main Menu
        MenuItem createSink = new MenuItem(GUIID_MENU_ITEM_SINK_CREATE, sinkCreateConstants.menu_SinkCreation(), new SinkCreatePlace());
        MenuItem sinksMenu = new MenuItem(GUIID_MENU_ITEM_SINKS_SHOW, sinksShowConstants.menu_Sinks(), new SinksShowPlace(),
                                          createSink);

        // Jobs Main Menu
        MenuItem jobsMenu = new MenuItem(GUIID_MENU_ITEM_JOBS_SHOW, jobsShowConstants.menu_Jobs(), new JobsShowPlace());

        // Toplevel Main Menu Container
        menuStructure = new MenuItem("toplevelmainmenu", "Toplevel Main Menu", NOWHERE,
                                     submittersMenu,
                                     flowsMenu,
                                     sinksMenu,
                                     jobsMenu);
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

    @Override
    public Activity getActivity(Place place) {
        if (place instanceof FlowCreatePlace) {
            return new FlowCreateActivity(
                /*
                 * (FlowCreatePlace) place,
                 */
                this);
        }
        if (place instanceof FlowComponentCreatePlace) {
            return new FlowComponentCreateActivity(
                /*
                 * (FlowComponentCreatePlace) place,
                 */
                this);
        }
        if (place instanceof SubmitterCreatePlace) {
            return new SubmitterCreateActivity(
                /*
                 * (SubmitterCreatePlace) place,
                 */
                this);
        }
        if (place instanceof FlowbinderCreatePlace) {
            return new FlowbinderCreateActivity(
                /*
                 * (FlowbinderCreatePlace) place,
                 */
                this);
        }
        if (place instanceof SinkCreatePlace) {
            return new SinkCreateActivity(
                /*
                 * (SinkCreatePlace) place,
                 */
                this);
        }
        if (place instanceof FlowComponentsShowPlace) {
            return new FlowComponentsShowActivity(
                /*
                 * (FlowComponentsShowPlace) place,
                 */
                this);
        }
        if (place instanceof FlowsShowPlace) {
            return new FlowsShowActivity(
                /*
                 * (FlowsShowPlace) place,
                 */
                this);
        }
        if (place instanceof SubmittersShowPlace) {
            return new SubmittersShowActivity(
                /*
                 * (SubmittersShowPlace) place,
                 */
                this);
        }
        if (place instanceof JobsShowPlace) {
            return new JobsShowActivity(
                /*
                 * (JobsShowPlace) place,
                 */
                this);
        }
        if (place instanceof SinksShowPlace) {
            return new SinksShowActivity(
                /*
                 * (SinksShowPlace) place,
                 */
                this);
        }
        if (place instanceof FlowBindersShowPlace) {
            return new FlowBindersShowActivity(
                /*
                 * (FlowBindersShowPlace) place,
                 */
                this);
        }
        return null;
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

    // Menu Structure
    @Override
    public MenuItem getMenuStructure() {
        return menuStructure;
    }

    // Views
    @Override
    public FlowCreateView getFlowCreateView() {
        return flowCreateView;
    }

    @Override
    public FlowComponentCreateView getFlowComponentCreateView() {
        return flowComponentCreateView;
    }

    @Override
    public SubmitterCreateView getSubmitterCreateView() {
        return submitterCreateView;
    }

    @Override
    public FlowbinderCreateView getFlowbinderCreateView() {
        return flowbinderCreateView;
    }

    @Override
    public SinkCreateView getSinkCreateView() {
        return sinkCreateView;
    }

    @Override
    public FlowComponentsShowView getFlowComponentsShowView() {
        return flowComponentsShowView;
    }

    @Override
    public FlowsShowView getFlowsShowView() {
        return flowsShowView;
    }

    @Override
    public SubmittersShowView getSubmittersShowView() {
        return submittersShowView;
    }

    @Override
    public JobsShowView getJobsShowView() {
        return jobsShowView;
    }

    @Override
    public SinksShowView getSinksShowView() {
        return sinksShowView;
    }

    @Override
    public FlowBindersShowView getFlowBindersShowView() {
        return flowBindersShowView;
    }

}
