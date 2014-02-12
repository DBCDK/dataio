package dk.dbc.dataio.gui.util;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import dk.dbc.dataio.gui.client.activities.CreateFlowActivity;
import dk.dbc.dataio.gui.client.activities.CreateFlowComponentActivity;
import dk.dbc.dataio.gui.client.activities.CreateFlowbinderActivity;
import dk.dbc.dataio.gui.client.activities.CreateSinkActivity;
import dk.dbc.dataio.gui.client.activities.CreateSubmitterActivity;
import dk.dbc.dataio.gui.client.activities.ShowFlowComponentsActivity;
import dk.dbc.dataio.gui.client.activities.ShowFlowsActivity;
import dk.dbc.dataio.gui.client.activities.ShowSinksActivity;
import dk.dbc.dataio.gui.client.activities.ShowSubmittersActivity;
import dk.dbc.dataio.gui.client.i18n.MainConstants;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowActivity;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowPlace;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowView;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowViewImpl;
import dk.dbc.dataio.gui.client.places.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.places.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.places.FlowCreatePlace;
import dk.dbc.dataio.gui.client.places.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.places.FlowsShowPlace;
import dk.dbc.dataio.gui.client.places.SinkCreatePlace;
import dk.dbc.dataio.gui.client.places.SinksShowPlace;
import dk.dbc.dataio.gui.client.places.SubmitterCreatePlace;
import dk.dbc.dataio.gui.client.places.SubmittersShowPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxy;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateView;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateViewImpl;
import dk.dbc.dataio.gui.client.views.FlowComponentsShowView;
import dk.dbc.dataio.gui.client.views.FlowComponentsShowViewImpl;
import dk.dbc.dataio.gui.client.views.FlowCreateView;
import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateView;
import dk.dbc.dataio.gui.client.views.FlowbinderCreateViewImpl;
import dk.dbc.dataio.gui.client.views.FlowsShowView;
import dk.dbc.dataio.gui.client.views.FlowsShowViewImpl;
import dk.dbc.dataio.gui.client.views.MenuItem;
import dk.dbc.dataio.gui.client.views.SinkCreateView;
import dk.dbc.dataio.gui.client.views.SinkCreateViewImpl;
import dk.dbc.dataio.gui.client.views.SinksShowView;
import dk.dbc.dataio.gui.client.views.SinksShowViewImpl;
import dk.dbc.dataio.gui.client.views.SubmitterCreateView;
import dk.dbc.dataio.gui.client.views.SubmitterCreateViewImpl;
import dk.dbc.dataio.gui.client.views.SubmittersShowView;
import dk.dbc.dataio.gui.client.views.SubmittersShowViewImpl;


public class ClientFactoryImpl implements ClientFactory {

    // Main Menu GUI Id's
    public final static String GUIID_MAIN_MENU_ITEM_SUBMITTERS = "mainmenuitemsubmitters";
    public final static String GUIID_MAIN_MENU_ITEM_FLOWS = "mainmenuitemflows";
    public final static String GUIID_MAIN_MENU_ITEM_SINKS = "mainmenuitemsinks";
    public final static String GUIID_MAIN_MENU_ITEM_JOBS = "mainmenuitemjobs";
    // Sub Menu GUI Id's
    public final static String GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION = "submenuitemsubmittercreation";
    public final static String GUIID_SUB_MENU_ITEM_FLOW_CREATION = "submenuitemflowcreation";
    public final static String GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION = "submenuitemflowcomponentcreation";
    public final static String GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW = "submenuitemflowcomponentsshow";
    public final static String GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION = "submenuitemflowbindercreation";
    public final static String GUIID_SUB_MENU_ITEM_SINK_CREATION = "submenuitemsinkcreation";

    public final static Place NOWHERE = null;
    private final static MainConstants constants = GWT.create(MainConstants.class);

    // Event Bus
    private final EventBus eventBus = new SimpleEventBus();

    // Place Controller
    private final PlaceController placeController = new PlaceController(eventBus);

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



    public ClientFactoryImpl() {
        // Submitters Main Menu
        MenuItem createSubmitter = new MenuItem(GUIID_SUB_MENU_ITEM_SUBMITTER_CREATION, constants.subMenu_SubmitterCreation(), new SubmitterCreatePlace());
        MenuItem submittersMenu = new MenuItem(GUIID_MAIN_MENU_ITEM_SUBMITTERS, constants.mainMenu_Submitters(), new SubmittersShowPlace(),
            createSubmitter);

        // Flows Main Menu
        MenuItem createFlow = new MenuItem(GUIID_SUB_MENU_ITEM_FLOW_CREATION, constants.subMenu_FlowCreation(), new FlowCreatePlace());
        MenuItem createFlowComponent = new MenuItem(GUIID_SUB_MENU_ITEM_FLOW_COMPONENT_CREATION, constants.subMenu_FlowComponentCreation(), new FlowComponentCreatePlace());
        MenuItem showFlowComponents = new MenuItem(GUIID_SUB_MENU_ITEM_FLOW_COMPONENTS_SHOW, constants.subMenu_FlowComponentsShow(), new FlowComponentsShowPlace());
        MenuItem createFlowBinder = new MenuItem(GUIID_SUB_MENU_ITEM_FLOWBINDER_CREATION, constants.subMenu_FlowbinderCreation(), new FlowbinderCreatePlace());
        MenuItem flowsMenu = new MenuItem(GUIID_MAIN_MENU_ITEM_FLOWS, constants.mainMenu_Flows(), new FlowsShowPlace(),
            createFlow,
            createFlowComponent,
            showFlowComponents,
            createFlowBinder);

        // Sinks Main Menu
        MenuItem createSink = new MenuItem(GUIID_SUB_MENU_ITEM_SINK_CREATION, constants.subMenu_SinkCreation(), new SinkCreatePlace());
        MenuItem sinksMenu = new MenuItem(GUIID_MAIN_MENU_ITEM_SINKS, constants.mainMenu_Sinks(), new SinksShowPlace(),
            createSink);

        // Jobs Main Menu
        MenuItem jobsMenu = new MenuItem(GUIID_MAIN_MENU_ITEM_JOBS, constants.mainMenu_Jobs(), new JobsShowPlace());

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
            return new CreateFlowActivity(/*(FlowCreatePlace) place,*/ this);
        }
        if (place instanceof FlowComponentCreatePlace) {
            return new CreateFlowComponentActivity(/*(FlowComponentCreatePlace) place,*/ this);
        }
        if (place instanceof SubmitterCreatePlace) {
            return new CreateSubmitterActivity(/*(SubmitterCreatePlace) place,*/ this);
        }
        if (place instanceof FlowbinderCreatePlace) {
            return new CreateFlowbinderActivity(/*(FlowbinderCreatePlace) place,*/ this);
        }
        if (place instanceof SinkCreatePlace) {
            return new CreateSinkActivity(/*(SinkCreatePlace) place,*/ this);
        }
        if (place instanceof FlowComponentsShowPlace) {
            return new ShowFlowComponentsActivity(/*(FlowComponentsShowPlace) place,*/ this);
        }
        if (place instanceof FlowsShowPlace) {
            return new ShowFlowsActivity(/*(FlowsShowPlace) place,*/ this);
        }
        if (place instanceof SubmittersShowPlace) {
            return new ShowSubmittersActivity(/*(SubmittersShowPlace) place,*/ this);
        }
        if (place instanceof JobsShowPlace) {
            return new JobsShowActivity(/*(JobsShowPlace) place,*/ this);
        }
        if (place instanceof SinksShowPlace) {
            return new ShowSinksActivity(/*(SinksShowPlace) place,*/ this);
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

}
